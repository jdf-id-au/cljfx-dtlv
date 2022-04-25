(ns user
  (:require [cljfx.api :as fx]
            [datalevin.core :as dtlv]))

(defonce dlc (dtlv/get-conn "store.dtlv"))
#_(dtlv/close dlc)

(defonce ctx (atom (fx/create-context {:ctx-ephemera [] :dtlv dlc})))

(dtlv/listen! dlc :ui (fn touch-ctx [_] (swap! ctx fx/swap-context assoc :max-tx (:max-tx @dlc))))
#_(dtlv/unlisten! dlc :ui)

(defn dlq-sub [ctx query-vec & args]
  (let [_ (fx/sub-val ctx :max-tx) ; <- this dependency causes automatic refresh via listen! above
        db @(fx/sub-val ctx :dtlv)]
    (apply dtlv/q query-vec (cons db args))))

(defn root [{:keys [:fx/context]}]
  {:fx/type :stage
   :width 500
   :height 500
   :showing true
   :title "Persistent vs ephemeral state"
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :children
                  [{:fx/type :v-box
                    :children
                    (into [{:fx/type :button :text "datalevin" :on-action {:type :dl-event}}]
                      (for [v (fx/sub-ctx context dlq-sub
                                '[:find [(pull ?e [*]) ...] :where [?e _ _]])]
                        {:fx/type :label :text (str v)}))}
                   {:fx/type :v-box
                    :children
                    (into [{:fx/type :button :text "context atom" :on-action {:type :ctx-event}}]
                      (for [v (fx/sub-val context :ctx-ephemera)]
                        {:fx/type :label :text (str v)}))}]}}})

(defn handler [{:keys [type]}]
  (case type
    :dl-event {:dl (rand)}
    :ctx-event {:ctx (rand)}))

(defn dl-effect
  "Return effect fn which updates disk store."
  [dlc]
  (fn [v _]
    (dtlv/transact! dlc [{:dl-ephemeron v}])))

(defn ctx-effect
  "Return effect fn which updates app context."
  [ctx]
  (fn [v _]
    (swap! ctx fx/swap-context update :ctx-ephemera conj v)))

(defn create-app
  "Factored out for controllable rebinding during dev."
  []
  (fx/create-app ctx
    :event-handler handler
    :desc-fn #(assoc % :fx/type root :dtlv dlc)
    :effects {:dl (dl-effect dlc)
              :ctx (ctx-effect ctx)}))

(defonce app (create-app))

(defn- rebind-app!
  "(dev convenience)"
  []
  (fx/unmount-renderer ctx (:renderer app))
  (def app (create-app)))

#_(rebind-app!)
