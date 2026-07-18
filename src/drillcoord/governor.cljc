(ns drillcoord.governor
  "DrillCoordGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  drilling-site scheduling/logistics coordination proposal an advisor
  may make for a well/drilling site under active or planned drilling
  operations. The governor never dispatches hardware itself, never
  operates drilling equipment, and never allows a proposal to
  authorize drilling to proceed, finalize a drilling-operation-
  execution decision, or override a drilling supervisor's/site-
  safety-officer's judgment — this actor coordinates DRILLING-SITE
  SCHEDULING/LOGISTICS ONLY. Modeled on cloud-itonami-isco-7232's
  aerocoord.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. site provenance         — the drilling site/well record must
                                 be independently verified/registered
                                 before any action.
    2. no-actuation            — proposal :effect must be :propose
                                 (the governor never dispatches
                                 hardware and never operates drilling
                                 equipment; it only gates what the
                                 advisor may coordinate).
    3. closed op-allowlist     — :op must be one of the four
                                 coordination ops (:log-work-record,
                                 :schedule-crew-operation,
                                 :flag-safety-concern,
                                 :coordinate-supply-order). No op that
                                 directly finalizes a drilling-
                                 operation-execution decision
                                 (authorizing drilling to proceed) or
                                 overrides a drilling supervisor's/
                                 site-safety-officer's judgment exists
                                 in this allowlist — these decision
                                 classes are structurally absent, not
                                 merely gated.
    4. site-mismatch           — if the proposal names a site, it
                                 must be the SAME site verified for
                                 this request (defense-in-depth
                                 against a proposal quietly targeting
                                 a different, unverified site).
    5. driller basis           — if the proposal references a
                                 driller, that driller must be a
                                 REGISTERED certified driller
                                 belonging to this site (an
                                 unregistered or foreign-site driller
                                 reference is not a routine scheduling
                                 proposal).
    6. scope-exclusion         — a proposal that attempts to
                                 authorize drilling to proceed, to
                                 finalize a drilling-operation-
                                 execution decision, or to override a
                                 drilling supervisor's or site-safety-
                                 officer's judgment, is a hard,
                                 PERMANENT block — never overridable by
                                 human approval, regardless of
                                 confidence or stake, and NEVER auto-
                                 commit-eligible under any confidence
                                 level. Detected as finalization/
                                 execution ACTION PHRASES (e.g.
                                 'authorize the drilling to proceed',
                                 'finalize the drilling operation',
                                 'override the drilling supervisor's
                                 judgment') in free-text proposal
                                 fields, never as bare domain nouns
                                 ('drill', 'rig', 'well', 'borehole',
                                 'spud') — bare-noun matching would
                                 false-trip on the default mock
                                 advisor's own routine rationale text,
                                 since this actor's entire domain is
                                 well drilling. See
                                 `drillcoord.governor-test`
                                 `default-mock-advisor-proposals-never-self-trip-scope-exclusion`.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off,
  regardless of confidence):
    7. :op :flag-safety-concern always escalates (a surfaced rig-
                                 condition/blowout-risk/equipment-
                                 condition concern always requires
                                 human review — the governor never
                                 resolves a safety concern itself, and
                                 this is unconditional — no
                                 confidence-level exception).
    8. :op :coordinate-supply-order with :cost above
                                 `supply-order-cost-threshold` always
                                 escalates.
    9. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [drillcoord.store :as store]))

(def confidence-floor 0.6)

(def supply-order-cost-threshold 20000)

(def ^:private allowed-ops
  #{:log-work-record :schedule-crew-operation :flag-safety-concern
    :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Scope-exclusion is matched as finalization/execution ACTION
;; PHRASES, never as bare nouns ("drill", "rig", "well", "borehole",
;; "spud") — this actor's entire domain is well drilling, so
;; bare-noun matching would false-trip on the default mock advisor's
;; own routine rationale text (e.g. "proposed :coordinate-supply-order
;; for site WELL-1" naming rig consumables, or a crew-schedule
;; proposal naming a well under drilling). See governor-test's
;; dedicated self-trip guard.
(def ^:private scope-exclusion-phrases
  ["authorize the drilling to proceed"
   "authorize drilling to proceed"
   "authorize the well to be drilled"
   "authorize the rig to spud"
   "clear the rig to spud"
   "clear the well for spud"
   "approve the well for spud"
   "declare the site clear to drill"
   "declare the well ready to drill"
   "finalize the drilling operation"
   "finalize the drilling-operation-execution decision"
   "finalize the drilling operation decision"
   "commence drilling operations directly"
   "commence drilling directly"
   "perform the drilling directly"
   "perform the drilling operation directly"
   "execute the drilling operation directly"
   "execute the drilling work directly"
   "dispatch the crew to begin drilling"
   "sign off the drilling authorization"
   "sign the drilling authorization"
   "issue the drilling authorization"
   "override the drilling supervisor's judgment"
   "override the drilling supervisor"
   "override the site safety officer's judgment"
   "override the site safety officer"
   "bypass the drilling supervisor"
   "bypass the site safety officer"
   "bypass the site safety review"])

(defn- scope-excluded-text [proposal]
  (str/lower-case (str (:rationale proposal) " " (:description proposal))))

(defn scope-exclusion-violation?
  "true if any free-text field of `proposal` contains a
  finalization/execution action phrase attempting to authorize
  drilling to proceed, finalize a drilling-operation-execution
  decision, or override a drilling supervisor's/site-safety-officer's
  judgment. Phrased as multi-word action phrases (never bare nouns) so
  this never false-trips on legitimate well-drilling domain
  vocabulary."
  [proposal]
  (let [text (scope-excluded-text proposal)]
    (boolean (some #(str/includes? text %) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [request proposal]} site-record d]
  (let [{:keys [op site-id driller-id]} proposal]
    (cond-> []
      (nil? site-record)
      (conj {:rule :no-site :detail "未登録 site/well record"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は掘削作業判断を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op :detail "closed op-allowlist 外の op（掘削作業の許可・実行決定の確定・掘削監督者/現場安全責任者の判断の上書きにあたる op は許可されていない）"})

      (and site-id (not= site-id (:site-id request)))
      (conj {:rule :site-mismatch :detail "proposal の site が request で検証済みの site と一致しない"})

      (and driller-id (nil? d))
      (conj {:rule :unknown-driller :detail "未登録 driller への提案は不可"})

      (and d (not= (:site-id d) (:site-id request)))
      (conj {:rule :driller-wrong-site :detail "driller が別 site 所属"})

      (scope-exclusion-violation? proposal)
      (conj {:rule :scope-exclusion-violation
             :detail "掘削の許可・掘削作業実行決定の確定・掘削監督者/現場安全責任者の判断の上書きにあたる提案は恒久的に禁止（human 承認でも上書き不可）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `drillcoord.store/Store`. Pure — never mutates
  the store, never dispatches a robot action, never operates drilling
  equipment."
  [request context proposal store]
  (let [site-record (store/site store (:site-id request))
        d (some->> (:driller-id proposal) (store/driller store))
        hard (hard-violations {:request request :proposal proposal} site-record d)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        cost (:cost proposal)
        over-threshold? (and (= :coordinate-supply-order (:op proposal))
                              (number? cost) (> cost supply-order-cost-threshold))
        always-risky? (or (contains? always-escalate-ops (:op proposal)) over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
