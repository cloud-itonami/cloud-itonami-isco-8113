# Operator Guide

## First Deployment

1. Define the operator's drilling site/well roster and driller-certification/registration process.
2. Define consent and purpose categories for logged work/drilling-log records.
3. Run synthetic coordination cases (work-record/drilling-log logging, scheduling, safety-concern flags, supply orders).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions — every safety-concern flag, no exceptions ever, and every above-threshold supply order.
5. Measure coordination outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (rig-condition, blowout-risk and equipment-condition concerns ALWAYS reach a human — no exceptions, ever)
- provenance for all drilling sites/wells and drillers before any coordination action
- human review for high-risk cases
- audit export for all gated actions

## Scope Boundary (Mandatory)

This actor coordinates drilling-site scheduling and logistics ONLY.
Operators must not wire this actor's output into any system that
would let it directly authorize drilling to proceed, finalize a
drilling-operation-execution decision, or override a drilling
supervisor's or site-safety-officer's judgment — the governor's closed
op-allowlist and scope-exclusion rule are the last line of defense,
not the only one; operator-side integrations must not create a path
around them.

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans unconditionally, and that no integration allows this actor to
authorize drilling to proceed, finalize a drilling-operation-execution
decision, or override drilling-supervisor/site-safety-officer
authority.
