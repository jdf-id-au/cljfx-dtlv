(ns user
  (:require [cljfx.api :as fx]
            [datalevin.core :as dtlv]))

;; Disk store but directly connected to UI
(defonce dlc (dtlv/get-conn "store.dtlv"))
#_(dtlv/close dlc)

;; Ephemera
(defonce ctx (atom (fx/create-context {:ctx-ephemera []})))
(defn touch-ctx
  "Pretend context has changed. Causes (seemingly selective, non-flashy) rerender."
  [& _] (swap! ctx fx/swap-context identity))

(defn root [{:keys [dtlv :fx/context]}]
  {:fx/type :stage
   :showing true
   :title "Persistent vs ephemeral state"
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :children
                  [{:fx/type :v-box
                    :children
                    (into [{:fx/type :button :text "datalevin" :on-action {:type :dl-event}}]
                      (for [v (dtlv/q '[:find [(pull ?e [*]) ...] :where [?e _ _]] @dtlv)]
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
  [context]
  (fn [v _]
    (swap! context fx/swap-context update :ctx-ephemera conj v)))

(defn create-app
  "Factored out for controllable rebinding during dev."
  []
  (fx/create-app ctx
    :event-handler handler
    :desc-fn #(assoc % :fx/type root :dtlv dlc)
    :co-effects {:dtlv #(deref dlc) :ctx #(deref ctx)}
    :effects {:dl (dl-effect dlc)
              :ctx (ctx-effect ctx)}))

(defonce app (create-app))

(defn- rebind-app!
  "(dev conveience)"
  []
  (fx/unmount-renderer ctx (:renderer app))
  (def app (create-app)))

(dtlv/listen! dlc :ui touch-ctx)
#_(dtlv/unlisten! dlc :ui)

#_(rebind-app!)
