(ns net.wikipunk.datomic
  "A simple component that connects to a datomic database using an
  injected :client.

  :conn -- datomic client connection
  :db-name -- name of the database"
  (:require
   [clojure.walk :as walk]
   [com.stuartsierra.component :as com]
   [datomic.client.api :as d]
   [datomic.client.api.protocols :as impl]
   [net.wikipunk.mop :as mop]
   [net.wikipunk.rdf :as rdf]))

(defrecord Connection [client db-name conn]
  com/Lifecycle
  (start [this]
    (d/create-database client this)
    (assoc this :conn (d/connect client this)))
  (stop [this]
    (assoc this :conn nil))

  impl/Db
  (as-of [_ time-point]
    (impl/as-of (d/db conn) time-point))
  (datoms [_ arg-map]
    (impl/datoms (d/db conn) arg-map))
  (db-stats [_]
    (impl/db-stats (d/db conn)))
  (history [_]
    (impl/history (d/db conn)))
  (index-pull [_ arg-map]
    (impl/index-pull (d/db conn) arg-map))
  (index-range [_ arg-map]
    (impl/index-range (d/db conn) arg-map))
  (pull [_ arg-map]
    (impl/pull (d/db conn) arg-map))
  (pull [_ selector eid]
    (impl/pull (d/db conn) selector eid))
  (since [_ t]
    (impl/since (d/db conn) t))
  (with [_ arg-map]
    (impl/with (d/db conn) arg-map))

  impl/Connection
  (db [_]
    (d/db conn))
  (transact [_ arg-map]
    (impl/transact conn arg-map))
  (sync [_ t]
    (impl/sync conn t))
  (tx-range [_ arg-map]
    (impl/tx-range conn arg-map))
  (with-db [_]
    (impl/with-db conn))

  impl/Client
  (administer-system [_ arg-map]
    (impl/administer-system client (merge {:db-name db-name} arg-map)))
  (list-databases [_ arg-map]
    (impl/list-databases client arg-map))
  (connect [_ arg-map]
    (impl/connect client (merge {:db-name db-name} arg-map)))
  (create-database [_ arg-map]
    (impl/create-database client (merge {:db-name db-name} arg-map)))
  (delete-database [_ arg-map]
    (impl/delete-database client (merge {:db-name db-name} arg-map))))

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
