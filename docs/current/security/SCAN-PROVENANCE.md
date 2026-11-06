# Deep Security Scan artifact reconciliation

Date: 3 November 2026. This is a read-only reconciliation of existing security-review
outputs, not a new scan or a validation of their findings. The canceled scans below are distinct from the two completed campaigns
indexed in [the security overview](README.md).

## Exact state

| Target revision | Manifest start/end (UTC) | Durable status | Retained records | Coverage |
| --- | --- | --- | --- | --- |
| App `1cc24ead`, original core `403c4683` | 2026-11-02 20:31:03 / 2026-11-03 05:18:47 | `canceled` | 87: 16 high, 55 medium, 16 low | `partial`; 385 surface entries, 125 deferred, 93 open questions |
| App `03feb66e`, core `f76940fc` | 2026-11-03 05:20:01 / 05:41:21 | `canceled` | 30: 1 high, 10 medium, 19 low | `partial`; 105 surface entries, 23 deferred, 15 open questions |

The first manifest ID is `0abee950-c616-43b2-9913-27def27aab63`; the
second is `68db7fdc-0353-4bcc-bf54-50309ec8654e`. The second coordinator
returned the exact terminal message: `Deep Scan
68db7fdc-0353-4bcc-bf54-50309ec8654e was canceled. Saved findings and
pending candidates remain available in the scan's retained results. Do not
start additional scan work or claim complete coverage.` No completion or
replacement scan was called. The first scan's 87 records include only 42
distinct rule IDs; the 16 high records include 11 rule IDs. Neither count is
the number of independently reproduced local bugs. Scan token-use metrics
are not present in the inspected manifests, so no token total is asserted.

Canonical private artifacts are retained outside the project and were not
copied into the public source tree. The scan IDs, source revisions and artifact
hashes in this report identify the original records. Each retained set contains
`scan-manifest.json`, `findings.json`, `coverage.json` and `report.md`.

| Artifact | Original SHA-256 | Later SHA-256 |
| --- | --- | --- |
| `scan-manifest.json` | `72ab95bdbbd18ac7e982639cc197a786a65ca5f12a6eb41e81dc44bc433dee63` | `cd53baf4a6466e39f9f4eddb27b09ef7ff80fc43aa646fdb363f0655c4a7d8dc` |
| `findings.json` | `f7871f4ca0848750f3c4ba95332f4a8c8f5b686fba829bde8aeaedb00941b2d5` | `9bac2b8c48ea617510d28b49968085327ef9efb4129b9af416a24552ea720684` |
| `coverage.json` | `303fcfa3a75afc70664e463371c29128809a82010d31e233e39b41a476281998` | `1684c6d912aaaf7077883fff00d4eea77f5eb631ec72f3fbc8aed902b73bbfd3` |
| `report.md` | `c7916ff22194cb27a9ec8935dd82636efbe60cff034d8575ee73c3ec101b9c53` | `a7879aed1b2b3a73ff2dcc6117c0ab0c095ec1eb8d4e00979286addd49bb06d1` |


These canceled scans establish partial static inventory only, not complete
security acceptance, independently reproduced cause counts or Android runtime
validation. Original sealed artifacts remain private.
