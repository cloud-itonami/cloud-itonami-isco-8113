(ns drillcoord.actor
  "DrillCoordActor — the ISCO-08 8113 well drillers and borers and
  related workers drilling-site scheduling/logistics coordination
  actor as a `langgraph.graph/state-graph` (ADR-2607121000 / CLAUDE.md
  Actors section). One graph run = one coordination operation request
  (intake → advise → govern → decide → commit/hold, with a
  human-approval interrupt for escalated proposals). No infinite
  internal loop; checkpointed per superstep so an interrupted run can
  resume after human sign-off. This actor coordinates DRILLING-SITE
  SCHEDULING/LOGISTICS ONLY — it never operates drilling equipment or
  performs drilling work itself and never commits a proposal the
  DrillCoordGovernor refuses. Modeled on cloud-itonami-isco-7232's
  aerocoord.actor.

  ```text
  :intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                             +-> :request-approval   (:escalate? true, interrupt-before)
                                             +-> :hold               (:hard? true)
  ```

  The unconditional invariant: the Well-Drilling Site Scheduling/
  Logistics Coordination Advisor can never directly commit a
  coordination record the DrillCoordGovernor refuses — every
  commit-record! call is gated behind `:decide`, and the governor's
  scope-exclusion rule makes any proposal to authorize drilling to
  proceed, finalize a drilling-operation-execution decision, or
  override a drilling supervisor's/site-safety-officer's judgment an
  unconditional, permanent :hold."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [drillcoord.advisor :as advisor]
            [drillcoord.governor :as governor]
            [drillcoord.store :as store]))

(defn build-graph
  "Build a compiled DrillCoordActor graph. `store` implements
  `drillcoord.store/Store`. `advisor` implements
  `drillcoord.advisor/Advisor` (defaults to `mock-advisor`).
  `checkpointer` defaults to an in-memory one."
  [{:keys [store advisor checkpointer]
    :or {advisor (advisor/mock-advisor)
         checkpointer (cp/mem-checkpointer)}}]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}
         :record      {:default nil}
         :audit       {:reducer into :default []}}})
      (g/add-node :intake (fn [s] s))
      (g/add-node :advise
                   (fn [{:keys [request]}]
                     (let [p (advisor/-advise advisor store request)]
                       {:proposal p
                        :audit [{:node :advise :request request :proposal p}]})))
      (g/add-node :govern
                   (fn [{:keys [request context proposal]}]
                     (let [v (governor/check request context proposal store)]
                       {:verdict v
                        :audit [{:node :govern :verdict v}]})))
      (g/add-node :decide
                   (fn [{:keys [verdict]}]
                     {:disposition (cond
                                     (:hard? verdict) :hold
                                     (:escalate? verdict) :request-approval
                                     :else :commit)}))
      (g/add-node :request-approval (fn [s] s))
      (g/add-node :commit
                   (fn [{:keys [request proposal]}]
                     (let [record {:site-id (:site-id request)
                                    :op (:op proposal)
                                    :driller-id (:driller-id proposal)
                                    :payload proposal}]
                       (store/commit-record! store record)
                       (store/append-ledger! store {:disposition :commit :record record})
                       {:record record
                        :audit [{:node :commit :record record}]})))
      (g/add-node :hold
                   (fn [{:keys [verdict]}]
                     (store/append-ledger! store {:disposition :hold :verdict verdict})
                     {:audit [{:node :hold :verdict verdict}]}))
      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)
      (g/add-conditional-edges
       :decide
       (fn [{:keys [disposition]}]
         (case disposition
           :commit :commit
           :request-approval :request-approval
           :hold)))
      (g/add-edge :request-approval :commit)
      (g/set-finish-point :commit)
      (g/set-finish-point :hold)
      (g/compile-graph {:checkpointer checkpointer
                         :interrupt-before #{:request-approval}})))

(defn run-request!
  "Run one operation request to completion or interrupt. `thread-id`
  scopes checkpointing for resume after human approval."
  [graph request context thread-id]
  (g/run* graph {:request request :context context} {:thread-id thread-id}))

(defn approve!
  "Human-in-the-loop resume: the interrupted `:request-approval` node
  advances straight to `:commit` on resume (approval is the act of
  resuming the thread). NOTE: `:request-approval` is only reachable
  when `:hard?` is false — the governor's scope-exclusion rule and
  other hard invariants route to `:hold` instead, so this resume path
  can never be used to approve a drilling-authorization decision or a
  drilling-supervisor's-/site-safety-officer's-judgment override."
  [graph thread-id]
  (g/run* graph nil {:thread-id thread-id :resume? true}))
