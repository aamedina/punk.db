(ns net.wikipunk.datomic
  "A simple component that connects to a datomic database using an
  injected :client.

  :conn -- datomic client connection
  :db-name -- name of the database"
  (:require
   [com.stuartsierra.component :as com]
   [datomic.client.api :as d]))

(defrecord Connection [client db-name]
  com/Lifecycle
  (start [this]
    (d/create-database client this)
    (assoc this :conn (d/connect client this)))
  (stop [this]
    (assoc this :conn nil)))
