# Segnali identificati su questa radio (ForFan / TS / Hazens)

Riepilogo di tutto ciò che DriveOS riesce a vedere su questa autoradio, con il canale esatto.
Tutti attivi di default: compaiono da soli sulla Home quando accadono (niente flag da mettere).
Questi dati sono anche nel file di export (schermata Veicolo → Esporta), sezione `identificati`.

## Letti dai canali pubblici (sempre, senza permessi speciali)
| Segnale | Canale | Valori |
|---|---|---|
| Fari | `prop:forfan_light_state` | accesi / spenti |
| Retromarcia | broadcast `com.forfan.operator_reversing` extra `operate` | 1 / 0 |
| Retrocamera | broadcast `com.ts.rearview_state` extra `show_state` | 1 / 0 |
| Telefono (vivavoce) | broadcast `com.globalconstant.BROADCAST_RESPONSE_HFPSTATUS` extra `hfpStatus` | 0 = scollegato |
| CarPlay / Android Auto | `prop:sys.speedplay.connection.state` | 1 / 0 |
| Navigatore di sistema | `prop:forfan.navi.name` | pacchetto (es. Google Maps) |
| App in primo piano | `prop:forfan.want2start` | pacchetto |

## Letti dal registro di sistema (serve il permesso READ_LOGS, concesso via ADB una volta)
| Segnale | Riga di log (app `com.ts.MainUI`) | Valori |
|---|---|---|
| Portiera aperta | `CanDoorMsgbox: SetShowFlg = true/false` | aperta / chiuse |
| Sensori di parcheggio | `CanFunc: mbRadarUIUpdate = true/false` | attivi / no |
| Luci (ADAS) | `MainUI: BROADCAST_ADAS_LIGHT_STATE on/off` | accese / spente |
| Radio FM (stazione) | riga con contesto FM/radio e frequenza | kHz, se compare |

## NON disponibili (confermato: logcat 27/10 + analisi GitHub)
- Carburante, temperatura liquido, olio, pressione pneumatici: gestiti dentro `com.ts.MainUI`
  tramite librerie native (JNI) sul canale MCU, non esposti ad altre app. Confermato dal
  progetto di riscrittura [Iscle/MainUI](https://github.com/Iscle/MainUI) e da
  [toczke/JunsunV1Plus](https://github.com/toczke/JunsunV1Plus).
  → Via affidabile: **OBD-II (ELM327 Bluetooth)**.
- Sintonizzazione radio indipendente: idem, è interna all'app Junsun. DriveOS la apre e la
  comanda con i tasti multimediali (come il volante), ma non la pilota da sola.

## Come esportare
Schermata **Veicolo → Esporta**: il JSON contiene `segnali` (grezzi), `identificati`
(questi, con valore attuale), `azioniScoperte` e `componentiFirmware`.
