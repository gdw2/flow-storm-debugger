(ns flow-storm.runtime.indexes.fn-call-stats-index
  (:require [flow-storm.runtime.indexes.protocols :as index-protos]
            [flow-storm.runtime.indexes.utils :refer [mh-put make-mutable-hashmap mh-contains? mh-get mh->immutable-map]]
            [flow-storm.runtime.types.fn-call-trace :as fn-call-trace]))

(deftype FnCall
    [^int form-id
     fn-name
     fn-ns]

  Object

  (hashCode [_]
    (let [p 31
          h (.hashCode fn-name)]
      (unchecked-add-int (unchecked-multiply-int p h) (.hashCode fn-ns))))

  (equals [this o]
    (if (identical? this o)
      true
      (if (or (nil? o) (not= (class this) (class o)))
        false
        (if (and (.equals ^String (.-fn-name this) ^String (.-fn-name o))
                 (.equals ^String (.-fn-ns this) ^String (.-fn-ns o)))
          true
          false)))))

(defrecord FnCallStatsIndex [stats]

  index-protos/BuildIndexP

  (add-form-init [_ _]) ; don't do anything for form-init
  
  (add-fn-call [_ fn-call]    
    (let [call (->FnCall (fn-call-trace/get-form-id fn-call)
                         (fn-call-trace/get-fn-name fn-call)
                         (fn-call-trace/get-fn-ns fn-call))]
      
      (if (mh-contains? stats call)
        (let [cnt (mh-get stats call)]          
          (mh-put stats call (inc cnt)))
        (mh-put stats call 1))))
  
  (add-expr-exec [_ _]) ; don't do anything for expr-exec
  
  (add-bind [_ _]) ; don't do anything for bind

  index-protos/FnCallStatsP

  (all-stats [_]    
    (reduce-kv (fn [r ^FnCall fc cnt]
                 (let [k {:form-id (.form-id fc)
                          :fn-name (.fn-name fc)
                          :fn-ns (.fn-ns fc)}]
                   (assoc r k cnt)))
               {}
               (mh->immutable-map stats))))

(defn make-index []
  (->FnCallStatsIndex (make-mutable-hashmap)))

