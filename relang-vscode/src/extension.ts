import * as path from 'path';
import * as net from 'net';
import { workspace, ExtensionContext, window, commands } from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    StreamInfo,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export function activate(context: ExtensionContext) {
    const config = workspace.getConfiguration('relang');
    const lspEnabled = config.get<boolean>('lsp.enabled', true);

    if (lspEnabled) {
        startLspClient(context);
    }

    // Register command to restart LSP
    context.subscriptions.push(
        commands.registerCommand('relang.restartLsp', async () => {
            if (client) {
                await client.stop();
            }
            startLspClient(context);
        })
    );

    // Register command to show ReLang version
    context.subscriptions.push(
        commands.registerCommand('relang.showVersion', () => {
            window.showInformationMessage('ReLang Extension v0.1.0');
        })
    );
}

async function startLspClient(context: ExtensionContext) {
    const config = workspace.getConfiguration('relang');
    const port = config.get<number>('lsp.port', 8123);

    const serverOptions = (): Promise<StreamInfo> => {
        return new Promise((resolve, reject) => {
            const socket = net.connect({ port, host: '127.0.0.1' });

            socket.on('connect', () => {
                resolve({
                    reader: socket,
                    writer: socket,
                });
            });

            socket.on('error', (err) => {
                window.showWarningMessage(
                    `Could not connect to ReLang LSP server on port ${port}. ` +
                    `Start it with: ./gradlew :relang-lsp:run --args="--port ${port}".`
                );
                reject(err);
            });
        });
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'relang' }],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.re'),
        },
    };

    client = new LanguageClient(
        'relang',
        'ReLang Language Server',
        serverOptions,
        clientOptions
    );

    try {
        await client.start();
        context.subscriptions.push(client);
    } catch (error) {
        // Connection failed - LSP features won't be available but syntax highlighting still works
        console.log('ReLang LSP not available - syntax highlighting only mode');
    }
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}
