(ns chainring-service.html-renderer
    "Module that contains functions used to render HTML pages sent back to the browser.")

(require '[hiccup.core  :as hiccup])
(require '[hiccup.page  :as page])
(require '[hiccup.form  :as form])

(defn render-html-header
    "Renders part of HTML page - the header."
    [url-prefix]
    [:head
        [:title "Chainring"]
        [:meta {:name "Author"    :content "Pavel Tisnovsky"}]
        [:meta {:name "Generator" :content "Clojure"}]
        [:meta {:http-equiv "Content-type" :content "text/html; charset=utf-8"}]
        (page/include-css (str url-prefix "bootstrap.min.css"))
        (page/include-css (str url-prefix "chainring.css"))
        (page/include-js  (str url-prefix "bootstrap.min.js"))
    ] ; head
)

(defn render-html-footer
    "Renders part of HTML page - the footer."
    []
    [:div "<br />"])

(defn render-navigation-bar-section
    "Renders whole navigation bar."
    [url-prefix]
    [:nav {:class "navbar navbar-inverse navbar-fixed-top" :role "navigation"} ; use navbar-default instead of navbar-inverse
        [:div {:class "container-fluid"}
            [:div {:class "row"}
                [:div {:class "col-md-7"}
                    [:div {:class "navbar-header"}
                        [:a {:href url-prefix :class "navbar-brand"} "Chainring"]
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

(defn render-front-page
    "Render front page of this application."
    [drawings]
    (page/xhtml
        (render-html-header "/")
        [:body
            [:div {:class "container"}
                (render-navigation-bar-section "/")
                (if drawings
                    [:table {:class "table table-stripped table-hover" :style "width:auto"}
                        [:tr [:th "ID"]
                             [:th "Výkres"]]
                        (for [drawing drawings]
                                [:tr [:td (:id drawing)]
                                     [:td (:name drawing)]])
                    ])
                (render-html-footer)
            ] ; </div class="container">
        ] ; </body>
))
