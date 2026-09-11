# cloud-itonami-isco-8113

Open Occupation Blueprint for **ISCO-08 8113**: Well Drillers and Borers and Related Workers.

This repository designs a forkable OSS business for a well-drilling drilling-site scheduling/logistics coordination service: a drilling-site scheduling/logistics coordination robot manages work-record/drilling-log logging, crew/shift scheduling, safety-concern flagging and drilling-equipment/consumables order coordination under a governor-gated actor, so the drilling operator keeps its own operating records instead of renting a closed drilling-scheduling SaaS.

**This actor coordinates DRILLING-SITE SCHEDULING/LOGISTICS ONLY — it never operates drilling equipment itself and never makes a drilling-operation-authorization decision.** Well drillers and borers operate drilling rigs for water wells and oil/gas exploratory drilling, where equipment hazards — blowout risk in oil/gas contexts, rig collapse, entanglement — are significant and comparable in stakes to other extractive-industry trades. The actor's closed op-allowlist contains no op that directly finalizes a drilling-operation-execution decision (authorizing drilling to proceed), nor overrides a drilling supervisor's/site-safety-officer's judgment. Any proposal that attempts either of these is a hard, permanent block, never overridable by human approval, and NEVER auto-commit-eligible under any confidence level.

**Maturity: `:implemented`.** `src/drillcoord/` implements the
`DrillCoordActor` as a `langgraph.graph/state-graph`
(`drillcoord.actor`) wired to a `Well-Drilling Site Scheduling/
Logistics Coordination Advisor` (`drillcoord.advisor`) and an
independent `DrillCoordGovernor` (`drillcoord.governor`), following
the itonami actor pattern (ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok? true) +-> :request-approval (:escalate? true, human-in-the-loop
interrupt) +-> :hold (:hard? true)`. See `kbb -M:test` output for
the current test/assertion counts.

HARD invariants (always `:hold`, never overridable): the drilling
site/well record must be independently verified/registered before any
action; a referenced driller must be a registered certified crew
member belonging to that site; `:effect` must be `:propose` only (no
hardware dispatch, no drilling equipment operated); the closed
op-allowlist is enforced (no op in the allowlist authorizes drilling
to proceed, finalizes a drilling-operation-execution decision, or
overrides drilling-supervisor/site-safety-officer authority); and any
proposal that attempts to authorize drilling to proceed, finalize a
drilling-operation-execution decision, or override a drilling
supervisor's/site-safety-officer's judgment is a hard, **permanent**
block — detected as finalization/execution action phrases (never bare
nouns like "drill"/"rig"/"well"/"borehole", which are ordinary
vocabulary for this domain and must not false-trip the guard).

Always-escalate ops (human sign-off regardless of confidence, mapping
this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (every surfaced rig-condition/blowout-risk/
equipment-condition concern, ALWAYS, no exceptions, ever) and
`:coordinate-supply-order` above the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a drilling-site scheduling/logistics coordination robot performs work-record/drilling-log logging, crew/shift-schedule proposals, safety-concern surfacing and drilling-equipment/consumables order coordination under an actor that proposes
actions and an independent **DrillCoordGovernor** that gates them. The governor never
dispatches hardware itself, never operates drilling equipment, never authorizes drilling to proceed, and never overrides a drilling supervisor's/site-safety-officer's judgment; `:high`/`:safety-critical` actions (such as a safety-concern flag or an above-threshold supply order) require human sign-off.

## Core Contract

```text
site roster + driller roster + rig schedule
        |
        v
Well-Drilling Site Scheduling/Logistics Coordination Advisor -> DrillCoordGovernor -> log record/schedule/order, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
authorize drilling to proceed, finalize a drilling-operation-execution
decision, override a drilling supervisor's/site-safety-officer's
judgment, suppress an operating record, or disclose sensitive data
without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8113`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
