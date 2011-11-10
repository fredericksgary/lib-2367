(ns lib-2367.core
  (:require [inflections.core :as inf]))

(def ^:private underscore-sym
  (comp symbol inf/underscore name))

(defmacro defbean
  [class-name
   field-names
   & interface-specs]
  (let [sym-base (str (gensym)),
        prefix-sym #(->> % name (str sym-base) symbol),
        setter-name
          (fn [field-name]
            (->> field-name name inf/capitalize (str sym-base "set") symbol)),
        setters
          (for [field-name field-names]
            `(defn ~(setter-name field-name)
              [this# v#]
              (swap!
                (. this# ~'state)
                assoc
                '~field-name
                v#))),
        interface-methods
          (for [meth interface-specs, :when (sequential? meth)]
            (let [[meth-name arg-list & body] meth]
              `(defn ~(prefix-sym meth-name)
                 ~arg-list
                 ; Create locals for all the field-names
                 (let [{:syms [~@field-names]} @(. ~(first arg-list) ~'state)]
                   ~@body))))]
    `(do
       ~@setters
       ~@interface-methods
       (defn ~(prefix-sym 'init)
         []
         [[] (atom {})])
       (gen-class
         :name ~(str (name (underscore-sym (ns-name *ns*))) "." class-name),
         :prefix ~sym-base,
         :state ~'state,
         :init ~'init,
         :implements [~@(filter symbol? interface-specs)],
         :methods
           [~@(for [field-name field-names]
                [(->> field-name name inf/capitalize (str "set") symbol)
                 [Object]
                 Object])]))))
