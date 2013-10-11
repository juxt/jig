;; Copyright © 2013, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns jig.jmx
  (:require
   jig
   [jig.reset :refer (reset-via-nrepl)]
   [clojure.java.jmx :as jmx]
   [clojure.tools
    [logging :refer :all]])
  (:import
   (jig Lifecycle)
   (javax.management Attribute AttributeList DynamicMBean MBeanInfo
                     ObjectName RuntimeMBeanException MBeanAttributeInfo MBeanOperationInfo MBeanParameterInfo)))

(deftype CustomMBean [ops]
  DynamicMBean
  (getMBeanInfo [b]
    (MBeanInfo. (.. b getClass getName)
                "System containing application state"
                nil
                nil
                (into-array MBeanOperationInfo
                            (for [[k v] ops]
                              (MBeanOperationInfo.
                               (name k) (:doc v) nil "java.lang.Object"
                               (get {:action MBeanOperationInfo/ACTION
                                     :action-info MBeanOperationInfo/ACTION_INFO
                                     :info MBeanOperationInfo/INFO
                                     :unknown MBeanOperationInfo/UNKNOWN
                                     }
                                    (:impact v)
                                    MBeanOperationInfo/UNKNOWN))))
                nil))
  (invoke [_ action params sig]
    (when-let [f (:fn (ops (keyword action)))]
      (f))))

(deftype JmxMBean [config]
  Lifecycle
  (init [_ system]
    system)
  (start [_ system]
    (jmx/register-mbean
     (CustomMBean. {:reset {:fn (fn [] (reset-via-nrepl system 20) "OK")
                            :doc "Reset"
                            :impact :action}})
     "jig.admin:name=System")
    system)
  (stop [_ system]
    (try
      (.unregisterMBean jmx/*connection* (jmx/as-object-name "jig.admin:name=System"))
      ;; JMX is a little sensitive.
      (catch Exception e))
    system))
