# Golden Resume Tests (obligatoire des S5)

## Objectif

Verifier qu'un run avec crash/resume produit les memes effets langage qu'un run nominal.

## Principe

Pour chaque scenario critique:

1. Run nominal jusqu'a completion.
2. Run avec crash force apres checkpoint N.
3. Resume depuis snapshot.
4. Comparer:
   - resultat final,
   - shape des failures,
   - transitions d'etat majeures.

## Oracles a comparer

- Valeur retour final.
- Etat final execution (`Completed`/`Failed`).
- Nombre de re-emissions d'effets externes (doit rester nul pour effets resolves).
- Integrite causale `Failure` (`cause`/`causes`).

## Scenarios minimaux

- `await` simple.
- `and` avec echec.
- `or` avec succes tardif.
- timer timeout pattern.
- signal receive suspendu puis resume.

## Livrables obligatoires

- Harness de crash injection.
- Golden traces versionnees.
- Rapport diff automatique (pass/fail).

