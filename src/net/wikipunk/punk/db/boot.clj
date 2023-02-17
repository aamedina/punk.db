(ns net.wikipunk.punk.db.boot
  {:rdf/type :jsonld/Context})

(def punk.db
  {:rdf/type    :rdfa/PrefixMapping
   :rdfa/uri    "https://wikipunk.net/punk/db/"
   :rdfa/prefix "punk.db"})
