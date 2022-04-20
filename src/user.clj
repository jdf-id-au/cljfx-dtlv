(ns user
  (:require [cljfx.api :as fx]
            [datalevin.core :as dl]))

(def con (dl/get-conn "store.dtlv"))

#_(dl/transact! con [{:a "hello there"}])
#_(dl/transact! con [{:b "hm" :c "blah"} {:d 123}])
#_(dl/q '[:find [(pull ?e [*]) ...] :where [?e _ _]] @con)

(defn root [{:keys [::store]}]
  {:fx/type :stage
   :showing true
   :title "hello"
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children
                  (into [{:fx/type :label :text "hmm"}])
                  (for [v (dl/q '[:find [(pull ?e [*]) ...] :where [?e _ _]] store)]
                    {:fx/type :label :text (str v)})}}})

(def renderer
  (fx/create-renderer))

(defn refresh-ui [{:keys [db-after]}]
  (renderer {:fx/type root ::store db-after}))

(dl/listen! con :ui refresh-ui)
#_(dl/unlisten! con :ui)

#_(renderer)
