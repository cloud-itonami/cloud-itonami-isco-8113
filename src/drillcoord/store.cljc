(ns drillcoord.store
  "SSoT for the ISCO-08 8113 well drillers and borers and related
  workers drilling-site scheduling/logistics coordination actor
  (itonami actor pattern, ADR-2607121000 / CLAUDE.md Actors section;
  README's 'Robotics premise' — a drilling-site scheduling/logistics
  coordination robot proposes crew/shift scheduling, work-record/
  drilling-log logging, safety-concern flags and drilling-equipment/
  consumables order coordination under this advisor/governor pair,
  which never dispatches hardware itself, never operates drilling
  equipment, and never finalizes a drilling-operation-execution
  decision or overrides a drilling supervisor's/site-safety-officer's
  judgment). Modeled on cloud-itonami-isco-7232's aerocoord.store.

  Domain:

    site     — a registered drilling site/well under active or
               planned drilling operation (:site-id, :name, :rig).
    driller  — a registered certified driller/rig crew member
               {:driller-id :site-id :name :role}, belonging to
               exactly one registered site (the site currently
               assigned to this driller for this drilling
               engagement).
    record   — a committed operating record (a logged work/drilling-
               log record, scheduling proposal, safety-concern flag
               or supply-order coordination entry) — written ONLY via
               commit-record!. This actor coordinates drilling-site
               scheduling/logistics ONLY — a `record` is a
               coordination artifact, never a drilling-operation-
               execution act, never an authorization for drilling to
               proceed, and never a drilling supervisor's/site-
               safety-officer's-judgment override.
    ledger   — append-only audit trail, commit or hold.")

(defprotocol Store
  (site [s site-id])
  (driller [s driller-id])
  (records-of [s site-id])
  (ledger [s])
  (register-site! [s st])
  (register-driller! [s d])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (site [_ site-id] (get-in @a [:sites site-id]))
  (driller [_ driller-id] (get-in @a [:drillers driller-id]))
  (records-of [_ site-id] (filter #(= site-id (:site-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-site! [s st]
    (swap! a assoc-in [:sites (:site-id st)] st) s)
  (register-driller! [s d]
    (swap! a assoc-in [:drillers (:driller-id d)] d) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:sites {} :drillers {} :records [] :ledger []}
                                   seed)))))
