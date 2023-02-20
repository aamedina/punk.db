(ns net.wikipunk.punk.db
  "Initialized to some WIP functions used to install RDF facts into a
  Datomic dev-local database based on the state of the system."
  (:require
   [clojure.datafy :refer [datafy]]
   [clojure.tools.logging :as log]
   [clojure.walk :as walk]
   [datomic.client.api :as d]
   [net.wikipunk.boot]
   [net.wikipunk.ext]
   [net.wikipunk.db.boot]
   [net.wikipunk.punk.db.boot]   
   [net.wikipunk.rdf :as rdf]))

(defprotocol Seed
  "Helper protocol to bootstrap attributes from loaded metaobjects."
  (select-attributes [x]))

(def ^:dynamic *boot-keys*
  [:db/id
   :db/ident
   :db/cardinality
   :db/valueType
   :db/isComponent
   :db/fulltext
   :db/tupleAttrs
   :db/tupleType
   :db/tupleTypes
   :db/fulltext
   :db/unique
   :owl/annotatedTarget
   :owl/sourceIndividual
   :owl/propertyChainAxiom
   :owl/members
   :owl/onProperty
   :owl/hasKey
   :owl/cardinality
   :owl/oneOf
   :owl/backwardCompatibleWith
   :owl/disjointWith
   :owl/assertionProperty
   :owl/withRestrictions
   :owl/targetValue
   :owl/priorVersion
   :owl/hasSelf
   :owl/equivalentProperty
   :owl/onDataRange
   :owl/targetIndividual
   :owl/onDatatype
   :owl/minCardinality
   :owl/propertyDisjointWith
   :owl/qualifiedCardinality
   :owl/maxQualifiedCardinality
   :owl/disjointUnionOf
   :owl/annotatedSource
   :owl/annotatedProperty
   :owl/unionOf
   :owl/distinctMembers
   :owl/maxCardinality
   :owl/imports
   :owl/incompatibleWith
   :owl/intersectionOf
   :owl/datatypeComplementOf
   :owl/equivalentClass
   :owl/someValuesFrom
   :owl/complementOf
   :owl/deprecated
   :owl/sameAs
   :owl/versionIRI
   :owl/onClass
   :owl/allValuesFrom
   :owl/versionInfo
   #_:owl/inverseOf
   :owl/hasValue
   :owl/differentFrom
   :owl/minQualifiedCardinality
   :owl/onProperties
   :rdfs/domain
   :rdfs/range
   :rdfs/seeAlso
   :rdfs/subPropertyOf
   :rdfs/subClassOf
   :rdfs/isDefinedBy
   :rdfs/member
   :rdfs/label
   :rdfs/comment
   :rdf/first
   :rdf/predicate
   :rdf/subject
   :rdf/object
   :rdf/rest
   :rdf/direction
   :rdf/value
   :rdf/type
   :rdf/language
   :rdfa/uri
   :rdfa/prefix
   :fressian/tag
   :schema/rangeIncludes
   :schema/domainIncludes])

(extend-protocol Seed
  clojure.lang.Namespace
  (select-attributes [ns]
    (when (isa? rdf/*classes* (:rdf/type (meta ns)) :owl/Ontology)
      (seq (keep select-attributes (vals (ns-publics ns))))))

  clojure.lang.Var
  (select-attributes [v]
    (when (map? @v)
      (select-attributes @v)))

  clojure.lang.Named
  (select-attributes [ident]
    (some-> (datafy ident)
            (select-attributes)))

  clojure.lang.IPersistentMap
  (select-attributes [m]
    (some->> (not-empty (select-keys m *boot-keys*))
             (walk/prewalk rdf/unroll-langString)
             (walk/prewalk rdf/unroll-blank)
             (walk/prewalk rdf/box-values)
             (walk/prewalk (fn [form]
                             (if (and (string? form)
                                      (>= (count form) 4096))
                               (subs form 0 4096)
                               form)))
             (walk/prewalk (fn [form]
                             (if (and (:rdfs/isDefinedBy form)
                                      (keyword? (:rdfs/isDefinedBy form)))
                               (update form :rdfs/isDefinedBy rdf/iri)
                               form)))))
  
  Boolean
  (select-attributes [bootstrap?]
    (let [xs (->> (all-ns)
                  (remove #(re-find #"^net.wikipunk.rdf.db" (name (ns-name %))))
                  (mapcat select-attributes))]
      (if bootstrap?
        ;; when bootstrapping filter for attributes
        (filter (every-pred :db/ident :db/cardinality :db/valueType) xs)
        ;; when not bootstrapping include all entities with :db/ident
        (filter :db/ident xs)))))

(defn select-classes
  ([h]
   (select-classes h false))
  ([h x]
   (->> (select-attributes x)
        (filter #(isa? h (:db/ident %) :rdfs/Class)))))

(defn select-properties
  ([h]
   (select-properties h false))
  ([h x]
   (->> (select-attributes x)
        (filter #(isa? h (:db/ident %) :rdf/Property)))))

(defn bootstrap
  "warning: very experimental

  requires datomic.client.api on the classpath"
  ([conn] (bootstrap rdf/*metaobjects* conn :force? false))
  ([h conn & {:keys [force?]}]
   (let [bootstrap? (or force? (== (:datoms (d/db-stats (d/db conn))) 217))
         tx-data    (select-attributes bootstrap?)
         root       *boot-keys*]
     (binding [*boot-keys* (if bootstrap?
                             [:db/id
                              :db/ident
                              :db/cardinality
                              :db/valueType
                              :db/isComponent
                              :db/fulltext
                              :db/tupleAttrs
                              :db/tupleType
                              :db/tupleTypes
                              :db/fulltext
                              :db/unique]
                             root)]
       (when bootstrap?
         (doseq [part (partition-all 512 tx-data)]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-attributes false)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-classes h)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-properties h)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (set! *boot-keys* root)
         (doseq [part (->> (select-properties h)
                           (remove :rdfs/subPropertyOf)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-properties h)
                           (filter :rdfs/subPropertyOf)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-classes h)
                           (remove :rdfs/subClassOf)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part}))
         (doseq [part (->> (select-classes h)
                           (filter :rdfs/subClassOf)
                           (map #(select-keys % *boot-keys*))
                           (partition-all 512))]
           (d/transact conn {:tx-data part})))))))

(defn test-bootstrap
  [h conn]
  (let [root *boot-keys*
        rf   (fn [db tx-data]
               (:db-after (d/with db {:tx-data [tx-data]})))]
    (binding [*boot-keys* [:db/id
                           :db/ident
                           :db/cardinality
                           :db/valueType
                           :db/isComponent
                           :db/fulltext
                           :db/tupleAttrs
                           :db/tupleType
                           :db/tupleTypes
                           :db/fulltext
                           :db/unique]]
      (as-> (reduce rf (d/with-db conn) (select-attributes true)) db
        (reduce rf db (select-attributes false))
        (reduce rf db (select-classes h))
        (reduce rf db (select-properties h))
        (do (set! *boot-keys* root) db)
        (reduce rf db (->> (select-properties h)
                           (remove :rdfs/subPropertyOf)))
        (reduce rf db (->> (select-properties h)
                           (filter :rdfs/subPropertyOf)))
        (reduce rf db (->> (select-classes h)
                           (remove :rdfs/subClassOf)))
        (reduce rf db (->> (select-classes h)
                           (filter :rdfs/subClassOf)))))))

(defmacro with-db
  "Evaluates body with mop/*env* bound to the return value of the db
  expression."
  [db & body]
  `(binding [mop/*env* ~db]
     (when-not (instance? datomic.core.db.Db mop/*env*)
       (throw (ex-info "*env* is not a datomic database" {:env mop/*env*})))
     ~@body))
