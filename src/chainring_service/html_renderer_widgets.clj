;
;  (C) Copyright 2017, 2018  Pavel Tisnovsky
;
;  All rights reserved. This program and the accompanying materials
;  are made available under the terms of the Eclipse Public License v1.0
;  which accompanies this distribution, and is available at
;  http://www.eclipse.org/legal/epl-v10.html
;
;  Contributors:
;      Pavel Tisnovsky
;

(ns chainring-service.html-renderer-widgets
    "Module that contains common utility functions for the html-renderer and html-renderer help modules

    Author: Pavel Tisnovsky")


(require '[hiccup.page  :as page])


(defn header
    "Renders part of HTML page - the header."
    [url-prefix & [options]]
    [:head
        [:title "Integrace CAD výkresů v RE-FX SAP"]
        [:meta {:name "Author"    :content "Pavel Tisnovsky"}]
        [:meta {:name "Generator" :content "Clojure"}]
        [:meta {:http-equiv "Content-type" :content "text/html; charset=utf-8"}]
        (page/include-css (str url-prefix "bootstrap/bootstrap.min.css"))
        (page/include-css (str url-prefix "chainring.css"))
        (page/include-css (str url-prefix "calendar.css"))
        (if (and options (:include-calendar? options))
            (page/include-js  (str url-prefix "bootstrap/bootstrap.min.js")))
        (if (and options (:include-calendar? options))
            (page/include-js  (str url-prefix "calendar_db.js")))
        (if (and options (:drawing-id options))
            [:script (str "var drawing_id = '" (:drawing-id options) "';")]
            [:script "var drawing_id = null;"])
        (if (and options (:floor-id options))
            [:script (str "var floor_id = '" (:floor-id options) "';")]
            [:script "var floor_id = null;"])
        (if (and options (:version options))
            [:script (str "var version = '" (:version options) "';")]
            [:script "var version = null;"])
        (if (and options (:raster-drawing-id options))
            [:script (str "var raster_drawing_id = '" (:raster-drawing-id options) "';")]
            [:script "var raster_drawing_id = null;"])
        (if (and options (:drawing-name options))
            [:script (str "var drawing_name = '" (:drawing-name options) "';")]
            [:script "var drawing_name = null;"])
        (if (and options (:sap-enabled options))
            [:script "var sap_enabled = true;"]
            [:script "var sap_enabled = false;"])
        (if (and options (:sap-url options))
            [:script (str "var sap_url = '" (:sap-url options) "';")]
            [:script "var sap_url = null;"])
        (if (and options (:include-drawing-js? options))
            (page/include-js (str url-prefix "drawing.js")))
    ] ; head
)


(defn navigation-bar
    "Renders whole navigation bar."
    [url-prefix]
    [:nav {:class "navbar navbar-inverse navbar-fixed-top" :role "navigation"} ; use navbar-default instead of navbar-inverse
        [:div {:class "container-fluid"}
            [:div {:class "row"}
                [:div {:class "col-md-7"}
                    [:div {:class "navbar-header"}
                        [:a {:href url-prefix :class "navbar-brand"} "Integrace CAD výkresů v RE-FX SAP"]
                    ] ; ./navbar-header
                    [:div {:class "navbar-header"}
                        [:ul {:class "nav navbar-nav"}
                            ;[:li (tab-class :whitelist mode) [:a {:href (str url-prefix "whitelist")} "Whitelist"]]
                        ]
                    ]
                ] ; col-md-7 ends
                ;[:div {:class "col-md-3"}
                ;    (render-name-field user-name (remember-me-href url-prefix mode))
                ;]
                ;[:div {:class "col-md-2"}
                ;    [:div {:class "navbar-header"}
                ;        [:a {:href (users-href url-prefix mode) :class "navbar-brand"} "Users"]
                 ;   ] ; ./navbar-header
                ;] ; col ends
            ] ; row ends
        ] ; </div .container-fluid>
]); </nav>


(defn footer
    "Renders part of HTML page - the footer."
    []
    [:div "<br /><br />&copy; eLevel system s.r.o."])


(defn back-button
    "Render back button widget."
    []
    [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"])


(defn help-button
    "Render help button widget."
    [help-page-url]
    [:a {:href help-page-url} [:img {:src "icons/help.gif"}]])
