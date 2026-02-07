package com.relang.lsp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal LSP session implementation focused on diagnostics publication.
 */
final class ReLangLspSession implements Closeable {

    private static final Gson GSON = new Gson();
    private static final String JSON_RPC = "2.0";

    private final Socket socket;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    private final Map<String, String> documents = new HashMap<>();
    private boolean shutdownRequested;

    ReLangLspSession(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    void run() throws IOException {
        while (true) {
            var body = readMessageBody();
            if (body == null) {
                return;
            }
            handleIncomingMessage(body);
        }
    }

    @Override
    public void close() throws IOException {
        out.flush();
        socket.close();
    }

    private void handleIncomingMessage(String body) throws IOException {
        final JsonObject message;
        try {
            var parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return;
            }
            message = parsed.getAsJsonObject();
        } catch (JsonParseException ignored) {
            return;
        }

        var method = readString(message.get("method"));
        var id = message.get("id");
        if (method == null) {
            return;
        }

        switch (method) {
            case "initialize" -> handleInitialize(id);
            case "initialized" -> {
                // No-op
            }
            case "textDocument/didOpen" -> handleDidOpen(message.getAsJsonObject("params"));
            case "textDocument/didChange" -> handleDidChange(message.getAsJsonObject("params"));
            case "textDocument/didClose" -> handleDidClose(message.getAsJsonObject("params"));
            case "textDocument/codeAction" -> handleCodeAction(id, message.getAsJsonObject("params"));
            case "shutdown" -> handleShutdown(id);
            case "exit" -> {
                if (!shutdownRequested) {
                    socket.close();
                }
                close();
            }
            default -> {
                if (id != null) {
                    sendError(id, -32601, "Method not supported: " + method);
                }
            }
        }
    }

    private void handleInitialize(JsonElement id) throws IOException {
        var capabilities = new JsonObject();
        var textDocumentSync = new JsonObject();
        textDocumentSync.addProperty("openClose", true);
        textDocumentSync.addProperty("change", 1); // Full sync
        capabilities.add("textDocumentSync", textDocumentSync);
        capabilities.addProperty("codeActionProvider", true);

        var result = new JsonObject();
        result.add("capabilities", capabilities);
        var serverInfo = new JsonObject();
        serverInfo.addProperty("name", "relang-lsp");
        serverInfo.addProperty("version", "0.1.0");
        result.add("serverInfo", serverInfo);

        sendResponse(id, result);
    }

    private void handleDidOpen(JsonObject params) throws IOException {
        if (params == null) return;
        var textDocument = params.getAsJsonObject("textDocument");
        if (textDocument == null) return;
        var uri = readString(textDocument.get("uri"));
        var text = readString(textDocument.get("text"));
        if (uri == null || text == null) return;

        documents.put(uri, text);
        publishDiagnostics(uri, text);
    }

    private void handleDidChange(JsonObject params) throws IOException {
        if (params == null) return;
        var textDocument = params.getAsJsonObject("textDocument");
        if (textDocument == null) return;
        var uri = readString(textDocument.get("uri"));
        if (uri == null) return;

        var changes = params.getAsJsonArray("contentChanges");
        if (changes == null || changes.isEmpty()) {
            return;
        }
        var lastChange = changes.get(changes.size() - 1).getAsJsonObject();
        var text = readString(lastChange.get("text"));
        if (text == null) {
            return;
        }

        documents.put(uri, text);
        publishDiagnostics(uri, text);
    }

    private void handleDidClose(JsonObject params) throws IOException {
        if (params == null) return;
        var textDocument = params.getAsJsonObject("textDocument");
        if (textDocument == null) return;
        var uri = readString(textDocument.get("uri"));
        if (uri == null) return;

        documents.remove(uri);
        var payload = new JsonObject();
        payload.addProperty("uri", uri);
        payload.add("diagnostics", new JsonArray());
        sendNotification("textDocument/publishDiagnostics", payload);
    }

    private void handleShutdown(JsonElement id) throws IOException {
        shutdownRequested = true;
        sendResponse(id, null);
    }

    private void handleCodeAction(JsonElement id, JsonObject params) throws IOException {
        if (id == null) {
            return;
        }

        var uri = params != null && params.get("textDocument") != null
                ? readString(params.getAsJsonObject("textDocument").get("uri"))
                : null;
        var diagnostics = params != null && params.get("context") != null
                ? params.getAsJsonObject("context").getAsJsonArray("diagnostics")
                : null;

        var actions = LspCodeActionMapper.toCodeActions(uri, diagnostics);
        sendResponse(id, actions);
    }

    private void publishDiagnostics(String uri, String text) throws IOException {
        var diagnostics = ReLangDiagnosticsEngine.analyze(uri, text);
        var payload = new JsonObject();
        payload.addProperty("uri", uri);

        var lspDiagnostics = new JsonArray();
        for (var diagnostic : diagnostics) {
            lspDiagnostics.add(LspDiagnosticMapper.toLspDiagnostic(uri, text, diagnostic));
        }
        payload.add("diagnostics", lspDiagnostics);
        sendNotification("textDocument/publishDiagnostics", payload);
    }

    private void sendResponse(JsonElement id, JsonElement result) throws IOException {
        var message = new JsonObject();
        message.addProperty("jsonrpc", JSON_RPC);
        message.add("id", id);
        if (result == null) {
            message.add("result", null);
        } else {
            message.add("result", result);
        }
        writeMessage(message);
    }

    private void sendError(JsonElement id, int code, String text) throws IOException {
        var message = new JsonObject();
        message.addProperty("jsonrpc", JSON_RPC);
        message.add("id", id);

        var error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", text);
        message.add("error", error);
        writeMessage(message);
    }

    private void sendNotification(String method, JsonObject params) throws IOException {
        var message = new JsonObject();
        message.addProperty("jsonrpc", JSON_RPC);
        message.addProperty("method", method);
        message.add("params", params);
        writeMessage(message);
    }

    private String readMessageBody() throws IOException {
        var headers = new HashMap<String, String>();
        String line;
        while ((line = readAsciiLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            var separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            var key = line.substring(0, separator).trim();
            var value = line.substring(separator + 1).trim();
            headers.put(key, value);
        }

        if (line == null && headers.isEmpty()) {
            return null;
        }

        var contentLengthValue = headers.get("Content-Length");
        if (contentLengthValue == null) {
            return null;
        }
        var contentLength = Integer.parseInt(contentLengthValue);
        var payload = in.readNBytes(contentLength);
        if (payload.length != contentLength) {
            return null;
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private void writeMessage(JsonObject message) throws IOException {
        var body = GSON.toJson(message);
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        writeAsciiLine("Content-Length: " + bytes.length);
        writeAsciiLine("");
        out.write(bytes);
        out.flush();
    }

    private String readAsciiLine() throws IOException {
        var buffer = new ByteArrayOutputStream(128);
        while (true) {
            var next = in.read();
            if (next == -1) {
                return buffer.size() == 0 ? null : buffer.toString(StandardCharsets.US_ASCII);
            }
            if (next == '\n') {
                var bytes = buffer.toByteArray();
                var length = bytes.length > 0 && bytes[bytes.length - 1] == '\r'
                        ? bytes.length - 1
                        : bytes.length;
                return new String(bytes, 0, length, StandardCharsets.US_ASCII);
            }
            buffer.write(next);
        }
    }

    private void writeAsciiLine(String line) throws IOException {
        out.write(line.getBytes(StandardCharsets.US_ASCII));
        out.write('\r');
        out.write('\n');
    }

    private static String readString(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
