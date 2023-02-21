(ns net.wikipunk.rdf.punk.db
  {:rdf/type :owl/Ontology})

(def Index
  {:db/ident :punk.db/Index
   :rdf/type :owl/Class})

(def SpatialIndex
  {:db/ident        :punk.db/SpatialIndex
   :rdf/type        :owl/Class
   :rdfs/subClassOf :punk.db/Index})

(def TemporalIndex
  {:db/ident        :punk.db/TemporalIndex
   :rdf/type        :owl/Class
   :rdfs/subClassOf :punk.db/Index})

(def FullTextIndex
  {:db/ident        :punk.db/FullTextIndex
   :rdf/type        :owl/Class
   :rdfs/subClassOf :punk.db/Index})
