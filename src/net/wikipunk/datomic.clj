(ns net.wikipunk.datomic
  "A simple component that connects to a datomic database using an
  injected :client.

  :conn -- datomic client connection
  :db-name -- name of the database"
  (:require
   [clojure.walk :as walk]
   [com.stuartsierra.component :as com]
   [datomic.client.api :as d]
   [net.wikipunk.mop :as mop]
   [net.wikipunk.rdf :as rdf]))

(defrecord Connection [client db-name]
  com/Lifecycle
  (start [this]
    (d/create-database client this)
    (assoc this :conn (d/connect client this)))
  (stop [this]
    (assoc this :conn nil)))

(defmethod mop/find-class-using-env [clojure.lang.Keyword datomic.core.db.Db]
  [ident env]
  (dissoc (walk/postwalk (fn [form]
                           (if (and (map? form)
                                    (contains? form :db/ident)
                                    (not= ident (:db/ident form)))
                             (:db/ident form)
                             form))
                         (d/pull env '[*] ident))
          :db/id))

(defmacro with-db
  "Evaluates body with mop/*env* bound to the return value of the db
  expression."
  [db & body]
  `(binding [mop/*env* ~db]
     (when-not (instance? datomic.core.db.Db mop/*env*)
       (throw (ex-info "*env* is not a datomic database" {:env mop/*env*})))
     ~@body))
