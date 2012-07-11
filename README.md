# clj-isa-protocol

This is a conversion of the `clojure.core/isa?` function to a protocol.  Since
multimethods use `isa?` internally, this could make multimethods a bit more
extensible at run time than they are now.

Also included is an implemention of multimethods as a protocol, stolen almost
entirely from Clojurescript.  It uses this library's `is-a?` function internally,
instead of `clojure.core/isa?`.

## Usage

Here we extend `is-a?`, and therefore our multimethod dispatch, to handle maps.
The `is-a?` function is defined in such a way that
`(is-a? specific-version-of-general-map general-map)` is true.  This is what
you want generally, e.g. `(core/isa? specific.Float general.Number)` or
`(core/isa? ::specifically-mantis ::cool-insect)`

```clojure
  (ns foo
    (:use [dispatch.is-a-protocol :only [is-a? Is-A]])
    (:require [dispatch.multimethods :as mm]))

  (defn- map-is-a? [child parent h]
    (and (map? parent)
         (let [sentinel (Object.)]
           (every? 
             (fn [[pk pv]]  ;; for every key in parent
               (let [cv (get child pk sentinel)]
                 (if (identical? cv sentinel)  ;; child has that key
                   false
                   (is-a? h cv pv))))  ;; and their values are related by is-a?
             parent))))
   
  (extend-protocol Is-A
    clojure.lang.IPersistentMap
    (-is-a? [c p h] (map-is-a? c p h)))
```

This now makes dispatching, say, Ring requests to more and more specific
handlers easy, and easy to extend:

```clojure
  (mm/defmulti handler "multimethod ring handler" (fn [req] req))
  (mm/defmethod handler {}
    [req] {:status 404 :headers {} :body "Not found."})
  (mm/defmethod handler {:request-method :get}
    [req] {:status 200 :headers {} :body "Generic GET."})
  (mm/defmethod handler {:request-method :get, :content-type "application/json"}
    [req] {:status 200 :headers {} :body "\"GET some json.\""})
  (mm/defmethod handler {:request-method :get, :content-type "application/xml"}
    [req] {:status 200 :headers {} :body "<foo>GET some XML.</foo>"})
  (mm/defmethod handler {:request-method :get,
                         :content-type "application/json"
                         :headers {"app/client-status" "favored-nation"}}
    [req] {:status 200 :headers {} :body "\"GET some super-special json.\""})
   
  (handler {:request-method :get})
  ;=> {:status 200, :headers {}, :body "Generic GET."}
  (handler {:request-method :post})
  ;=> {:status 404, :headers {}, :body "Not found."}
  (handler {:request-method :get :content-type "application/xml"})
  ;=> {:status 200, :headers {}, :body "<foo>GET some XML.</foo>"}
  (handler {:request-method :get :content-type "application/json"})
  ;=> {:status 200, :headers {}, :body "\"GET some json.\""}
  (handler {:request-method :get :headers {"anything" "really"} :content-type "application/json"})
  ;=> {:status 200, :headers {}, :body "\"GET some json.\""}
  (handler {:request-method :get :headers {"anything" "really", "app/client-status" "favored-nation"} :content-type "application/json"})
  ;=> {:status 200, :headers {}, :body "\"GET some super-special json.\""}
```

Notice that while you could have done something similar with clojure.core's
multimethods, it's not quite as flexible:

```clojure
  (core/defmulti handler "multimethod ring handler" (juxt :request-method :content-type))
  (core/defmethod handler [:get "application/json"] ...)
  ;; Oh, wait, now I want to do special things depending on the headers...
  ;; But I can't, unless I change the implementation of the above method.
  
  ;; Or, less likely, change the dispatch fn and value for all methods:
  (core/defmulti handler "multimethod ring handler"
    (juxt :request-method :content-type #(get-in % [:headers "special"])))
  (core/defmethod handler [:get "application/json" nil] ...)
  ;; and the same for lots of methods... 
```

This is similar to the way that document DBs are more flexible than relational
DBs about the data model.

## Performance

`is-a?` is actually faster than `clojure.core/isa?` in many cases.  But let's
be honest, if you're using multimethods, you're not optimizing for speed anyway.

## License

Copyright © 2012 Leif Poorman

Distributed under the Eclipse Public License, the same as Clojure.
