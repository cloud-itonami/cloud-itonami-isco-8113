# Business Model: Well-Drilling Site Scheduling & Logistics Coordination Service

## Classification

- Repository: `cloud-itonami-isco-8113`
- ISCO-08: `8113`
- Occupation: Well Drillers and Borers and Related Workers
- Social impact: worker-safety, public-safety, environmental-safety

## Scope

**This actor coordinates drilling-site scheduling and logistics
only.** It never operates drilling equipment itself, never authorizes
drilling to proceed, never finalizes a drilling-operation-execution
decision, and never overrides a drilling supervisor's or site-safety-
officer's judgment. Well drillers and borers operate drilling rigs for
water wells and oil/gas exploratory drilling — equipment hazards
(blowout risk in oil/gas contexts, rig collapse, entanglement) are
significant, comparable in stakes to other extractive-industry trades
— so every proposal this actor's advisor can make is limited to
coordination, not execution and not operation-authorization.

## Customer

- water-well and oil/gas exploratory drilling contractors
- drilling-rig crews and site supervisors

## Offer

- work-record/drilling-log logging (task, drilling progress, mud
  weight/pressure readings, materials usage)
- crew/shift-schedule scheduling proposals
- safety-concern surfacing (rig-condition, blowout-risk, equipment-
  condition observation)
- drilling-equipment/consumables supply-order coordination

## Revenue

- monthly coordination-platform retainer
- per-site/per-rig logistics fee

## Trust Controls

- no drilling-operation-execution decision (authorizing drilling to
  proceed) is ever finalized by this actor
- no drilling supervisor's or site-safety-officer's judgment is ever
  overridden by this actor
- every safety-concern flag ALWAYS escalates to human sign-off, no
  exceptions, ever
- supply orders above the registered cost threshold always escalate to
  human sign-off
- site/well and driller provenance is independently verified before
  any coordination action
- coordination and audit records are auditable, not editable
