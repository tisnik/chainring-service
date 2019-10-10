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

(ns chainring-service.html-renderer
    "Module that contains functions used to render HTML pages sent back to the browser.

    Author: Pavel Tisnovsky")


(require '[hiccup.page  :as page])
(require '[hiccup.form  :as form])

(require '[chainring-service.html-renderer-widgets :as widgets])
(require '[chainring-service.drawing-utils         :as drawing-utils])



(defn render-front-page
    "Render front page of this application."
    [valid-from]
    (page/xhtml
        (widgets/header "/" {:include-calendar? true})
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h3 "Vstup do systému"]
                (form/form-to {:name "inputForm"} [:get "/areals"]
                    [:span "Platnost od:"] "&nbsp;"
                    [:script "new tcal ({'formname': 'inputForm', 'controlname': 'valid-from'});"]
                    [:br]
                    [:br]
                    [:input {:type "text" :id "valid-from" :readonly "readonly" :name "valid-from" :value valid-from :style "width:10em"}]
                    "&nbsp;"
                    [:a {:href "/help_valid_from_settings"} [:img {:src "icons/help.gif"}]]
                    [:br]
                    [:br]
                    [:button {:type "submit" :class "btn btn-success" :style "width:10em"} "Seznam areálů"]
                )
                [:div {:style "height: 10ex"}]
                [:h3 "Další volby"]
                ;[:a {:href "settings" :class "btn btn-default" :role "button" :style "width:10em"} "Nastavení"]
                ;[:br]
                [:br]
                ;[:a {:href "db-stats" :class "btn btn-default" :role "button" :style "width:10em"} "Stav databáze"]
                ;"&nbsp;"
                [:a {:href "drawings-stats" :class "btn btn-default" :role "button" :style "width:10em"} "Stav výkresů"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn passive-color-box
    "Render 'passive' color box into HTML page."
    [html-color]
    [:div {:class "color-box" :style (str "background-color:" html-color "; display:inline-block")}])


(defn color-box
    "Render 'active' color box into HTML page."
    [html-color value]
    [:a {:href "#" :onclick
        (str "document.getElementById('selected-room-color').value='" value "';"
             "document.getElementById('selected-room-color-code').value='" html-color "'")}
        (passive-color-box html-color)])


(defn render-settings-page
    "Render page with settings/configuration."
    [user-id]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nastavení"]
                (form/form-to [:post "store-settings"]
                    [:input {:type "hidden" :name "selected-room-color-code" :id "selected-room-color-code" :value "xxx"}]
                    [:fieldset {:class "form-group"}
                        [:label {:for "user-id"} "ID uživatele"]
                        [:input {:type "text" :class "form-control" :style "width:10em" :readonly "readonly" :id "user-id" :name "user-id" :value user-id}]
                    ]
                    [:fieldset {:class "form-group"}
                        [:label {:for "resolution"} "Výchozí velikost půdorysu"]
                        [:br]
                        (form/drop-down "resolution" ["320x240", "400x300", "640x480" "800x600" "1024x768"])
                    ]
                    [:fieldset {:class "form-group"}
                        [:label {:for "selected-room-color"} "Barva výplně vybrané místnosti"]
                        [:br]
                        (color-box "#ff0000" "červená")
                        (color-box "#ff8000" "oranžová")
                        (color-box "#ffff00" "žlutá")
                        (color-box "#80ff80" "zelená")
                        (color-box "#80ffff" "modrozelená")
                        (color-box "#8080ff" "modrá")
                        (color-box "#ff80ff" "fialová")
                        (color-box "#a0a0a0" "šedá")
                        (color-box "#ffffff" "bez výplně")
                        [:input {:type "text" :class "form-control" :style "width:10em" :readonly "readonly" :id "selected-room-color" :name "selected-room-color" :value ""}]
                    ]
                    [:fieldset {:class "form-group"}
                        [:label {:for "selected-room-color"} "Šířka obrysu vybrané místnosti"]
                        [:br]
                        (form/drop-down "pen-width" [["1 pixel" 1] ["2 pixely" 2] ["3 pixely" 3]])
                    ]
                    [:br]
                    [:br]
                    [:button {:type "submit" :class "btn btn-success"} "Uložit nastavení"]
                )
                [:br]
                (form/form-to [:get "/"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))

; (form/text-field {:size "40" :class "form-control" :placeholder "Search for word"} "word" (str word))
; (form/drop-down "class" ["N/A" "Noun" "Verb" "Adjective" "Adverb" "Pronoun" "Preposition" "Conjunction" "Determiner" "Exclamation"])]]
; (form/check-box "internal")]]


(defn render-db-statistic-page
    "Render page with database statistic."
    [last-update areals buildings floors rooms drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Stav databáze"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Typ objektu"]          [:th "Počet záznamů"]]
                    [:tr [:td "Areály"]               [:td areals]]
                    [:tr [:td "Budovy"]               [:td buildings]]
                    [:tr [:td "Podlaží"]              [:td floors]]
                    [:tr [:td "Místnosti"]            [:td rooms]]
                    [:tr [:td "Výkresy"]              [:td drawings]]
                    ;[:tr [:td "Data výkresů"]           [:td (:drawings-data db-stats)]]
                    ;[:tr [:td "Místnosti na výkresech"] [:td (:drawing-rooms db-stats)]]
                    ;[:tr [:td "Uživatelé"]              [:td (:users db-stats)]]
                    [:tr [:td "&nbsp;"] [:td "&nbsp;"]]
                    [:tr [:th "Poslední aktualizace"] [:td last-update]]
                ]
                [:br]
                (form/form-to [:get "/"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-drawings-statistic-page
    "Render page with drawings statistic."
    [drawings-count json-count binary-count]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Stav výkresů"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Formát"]        [:th "Počet výkresů"] [:th ""]]
                    ;[:tr [:td "Drw"]           [:td drawings-count] [:td [:a {:href "/drawings-list"} "seznam"]]]
                    [:tr [:td "JSON"]          [:td json-count] [:td [:a {:href "/json-list"} "seznam výkresů"]]]
                    ;[:tr [:td "Binary"]        [:td binary-count] [:td [:a {:href "/binary-list"} "seznam"]]]
                ]
                [:br]
                (form/form-to [:get "/"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-drawings-list
    "Render page with list of drawings."
    [drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Seznam výkresů ve formátu Drw"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Výkres"] [:th "Velikost"]]; [:th {:colspan "2"} "Náhled"]]
                    (for [drawing drawings]
                        [:tr [:td (.getName drawing)]
                             [:td (.length drawing) " B"]
                             ;[:td [:a {:href (str "/drawing-preview?drawing-name=" (.getName drawing))} [:img {:src "icons/draw.png"}]]]
                             ;[:td [:a {:href (str "/raster-preview?drawing-name=" (.getName drawing))} [:img {:src "icons/image.png"}]]]
                    ])
                ]
                [:br]
                (form/form-to [:get "/drawings-stats"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-json-list
    "Render list of JSON files containing drawings."
    [drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Seznam výkresů ve formátu JSON"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Výkres"] [:th "Velikost"] [:th "Náhled"]]
                    (for [drawing drawings]
                        [:tr [:td (.getName drawing)]
                             [:td (.length drawing) " B"]
                             [:td [:a {:href (str "/raster-preview?drawing-name=" (.getName drawing))} [:img {:src "icons/image.png"}]]]]
                    )
                ]
                [:br]
                (form/form-to [:get "/drawings-stats"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-binary-list
    "Render list of binary files containing drawings."
    [drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Seznam výkresů v binárním formátu"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Výkres"] [:th "Velikost"] [:th "Náhled"]]
                    (for [drawing drawings]
                        [:tr [:td (.getName drawing)]
                             [:td (.length drawing) " B"]
                             [:td [:a {:href (str "/raster-preview?drawing-name=" (.getName drawing))} [:img {:src "icons/image.png"}]]]]
                    )
                ]
                [:br]
                (form/form-to [:get "/drawings-stats"]
                    [:button {:type "submit" :class "btn btn-primary"} "Zpět"]
                )
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-areals-list
    "Render list of areals."
    [valid-from areals-from areals]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Seznam areálů"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"]
                         [:th "Jméno"]
                         [:th "Platnost od"]
                         [:th ""]]
                    (for [areal areals]
                            [:tr [:td (:AOID areal)]
                                 [:td [:a {:href (str "areal?areal-id=" (:AOID areal) "&valid-from=" valid-from)}(:Label areal)]]
                                 [:td areals-from]
                                 [:td [:a {:title "Podrobnější informace o areálu"
                                           :href (str "areal-info?areal-id=" (:AOID areal) "&valid-from=" valid-from)}
                                           [:img {:src "icons/info.gif"}]]]])
                    [:tr [:td {:colspan 4} "&nbsp;"]]
                    [:tr [:td {:colspan 2} "Zadaná platnost od:"]
                         [:td valid-from]
                         [:td (widgets/help-button "/help_valid_from")]]
                ]
                (widgets/back-button)
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-areal-info
    "Render info about selected areal."
    [project-id project-info building-count valid-from]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Informace o areálu '" (or (:Label project-info) (:AOID project-info)) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "AOID"]
                         [:td project-id]
                         [:td [:a {:href "/help_aoid_areal"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Jméno"]
                         [:td (:Label project-info)]
                         [:td [:a {:href "/help_name_areal"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:th "Začátek platnosti"]
                         [:td (:valid-from project-info)]
                         [:td [:a {:href "/help_valid_from_areal"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Konec platnosti"]
                         [:td (:valid-to project-info)]
                         [:td [:a {:href "/help_valid_to_areal"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:td "Interní identifikátor"]
                         [:td (:ID project-info)]
                         [:td [:a {:href "/help_intreno"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td "Zadaná platnost od:"]
                         [:td valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                (widgets/back-button)
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn get-building-name
    "Get the name of building."
    ( [building-info]
      (if (empty? (:Label building-info))
          (:AOID building-info)
          (:Label building-info)))
    ( [building-info default-label]
      (if (empty? (:Label building-info))
          default-label
          (:Label building-info))))


(defn render-building-info
    "Render information about building."
    [building-id building-info valid-from]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Informace o budově '" (get-building-name building-info) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "AOID"]
                         [:td building-id]
                         [:td [:a {:href "/help_aoid_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Jméno"]
                         [:td (get-building-name building-info "nezadáno")]
                         [:td [:a {:href "/help_name_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Funkce"]
                         [:td (:Function building-info)]
                         [:td [:a {:href "/help_function_areal"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:th "Začátek platnosti"]
                         [:td (:valid-from building-info)]
                         [:td [:a {:href "/help_valid_from_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Konec platnosti"]
                         [:td (:valid-to building-info)]
                         [:td [:a {:href "/help_valid_to_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:td "Interní identifikátor"]
                         [:td (:ID building-info)]
                         [:td [:a {:href "/help_intreno"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td "Zadaná platnost od:"]
                         [:td valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-floor-info
    "Render information about floor."
    [floor-id floor-info drawing-count valid-from]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Informace o podlaží '" (or (:Label floor-info) (:AOID floor-info)) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"]
                         [:td floor-id]
                         [:td [:a {:href "/help_aoid_floor"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Jméno"]
                         [:td (:Label floor-info)]
                         [:td [:a {:href "/help_name_floor"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Začátek platnosti"]
                         [:td (:valid-from floor-info)]
                         [:td [:a {:href "/help_valid_from_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:th "Konec platnosti"]
                         [:td (:valid-to floor-info)]
                         [:td [:a {:href "/help_valid_to_building"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:th "Počet verzí výkresů"]
                         [:td (or drawing-count "nelze zjistit")]
                         [:td [:a {:href "/help_drawing_count_floor"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:td "Interní identifikátor"]
                         [:td (:ID floor-info)]
                         [:td [:a {:href "/help_intreno"} [:img {:src "icons/help.gif"}]]]]
                    [:tr [:td "Zadaná platnost od:"]
                         [:td valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-drawing-info
    "Render information about drawing."
    [drawing-id drawing-info]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Informace o výkresu '" drawing-id "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID výkresu"]      [:td drawing-id] [:td "&nbsp;"]]
                    [:tr [:th "Vytvořeno"]       [:td (:created drawing-info)]]
                    [:tr [:th "Verze formátu"]   [:td (:format-version drawing-info)]]
                    [:tr [:th "Počet místností"] [:td (:rooms-count drawing-info)]]
                    [:tr [:td {:colspan 2} "&nbsp;"]]
                    [:tr [:th "Celkový počet entit"] [:td {:style "text-align:right"} (-> drawing-info :entities-count :all)]]
                    [:tr [:td "&nbsp;&nbsp;&nbsp;&nbsp;úseček"]  [:td {:style "text-align:right"} (-> drawing-info :entities-count :lines)]]
                    [:tr [:td "&nbsp;&nbsp;&nbsp;&nbsp;kružnic"] [:td {:style "text-align:right"} (-> drawing-info :entities-count :circles)]]
                    [:tr [:td "&nbsp;&nbsp;&nbsp;&nbsp;oblouků"] [:td {:style "text-align:right"} (-> drawing-info :entities-count :arcs)]]
                    [:tr [:td "&nbsp;&nbsp;&nbsp;&nbsp;textů"]   [:td {:style "text-align:right"} (-> drawing-info :entities-count :texts)]]
                    [:tr [:td "&nbsp;&nbsp;&nbsp;&nbsp;polyčar"] [:td {:style "text-align:right"} (-> drawing-info :entities-count :polylines)]]
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-building-list
    "Render list of buildings for selected project (areal)."
    [project-id project-info buildings valid-from]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Seznam budov v areálu '" (:AOID project-info) "'")]
                [:h4 (:Label project-info)]
                [:br]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"]
                         [:th "Budova"]
                         ;[:th "AOID"]
                         ;[:th "Vytvořeno"]
                         [:th ""]]
                    (for [building buildings]
                            [:tr [:td (:AOID building)]
                                 [:td [:a {:href (str "building?areal-id=" project-id "&building-id=" (:AOID building) "&valid-from=" valid-from)}
                                          (if (empty? (:Label building)) (:AOID building) (:Label building))]]
                                 [:td [:a {:title "Podrobnější informace o budově"
                                           :href (str "building-info?areal-id=" project-id "&building-id=" (:AOID building) "&valid-from=" valid-from)}
                                           [:img {:src "icons/info.gif"}]]]])
                    [:tr [:td {:colspan 3} "&nbsp;"]]
                    [:tr [:td "Zadaná platnost od:"]
                         [:td valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-floor-list
    "Render list of floors for selected building."
    [project-id building-id project-info building-info floors valid-from]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Seznam podlaží v areálu '" (:Label project-info) "' a budově '" (get-building-name building-info) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr
                        [:th "Areál"]  [:td (:Label project-info)]
                        [:th "AOID"]   [:td (:AOID project-info)]
                        [:td [:a {:title "Podrobnější informace o areálu"
                                  :href (str "areal-info?areal-id=" project-id "&valid-from=" valid-from)}
                                  [:img {:src "icons/info.gif"}]]]]
                    [:tr
                        [:th "Budova"] [:td (get-building-name building-info "nezadáno")]
                        [:th "AOID"]   [:td (:AOID building-info)]
                        [:td [:a {:title "Podrobnější informace o budově"
                                  :href (str "building-info?areal-id=" project-id "&building-id=" building-id "&valid-from=" valid-from)}
                                  [:img {:src "icons/info.gif"}]]]]
                    [:tr [:td {:colspan 5} "&nbsp;"]]
                    [:tr [:td {:colspan 2} "Zadaná platnost od:"]
                         [:td {:colspan 2} valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                [:br]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"]
                         [:th "Podlaží"]
                         ;[:th "AOID"]
                         ;[:th "Vytvořeno"]
                         ;[:th "Výkresů"]
                         [:th ""]]
                    (for [floor floors]
                            [:tr [:td (:AOID floor)]
                                 [:td [:a {:href (str "floor?areal-id=" project-id "&building-id=" building-id "&floor-id=" (:AOID floor) "&valid-from=" valid-from)} (get-building-name floor)]]
                                 ;[:td (:aoid floor)]
                                 ;[:td (:created floor)]
                                 ;[:td
                                 ;(if (zero? (:drawings floor))
                                 ;    [:div {:class "no-drawings"} 0]
                                 ;    [:div {:class "has-drawings"} (:drawings floor)])
                                 ;]
                                 [:td [:a {:title "Podrobnější informace o podlaží"
                                           :href (str "floor-info?areal-id=" project-id "&building-id=" building-id "&floor-id=" (:AOID floor) "&valid-from=" valid-from)}
                                           [:img {:src "icons/info.gif"}]]]])
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn sap-href
    "Construct HREF to SAP for a selected room."
    [room]
    "")


(defn render-room-list-page
    "Render list of rooms for the selected floor."
    [floor-id floor-info version rooms]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Informace místnostech na podlaží '" (:name floor-info) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"] [:td floor-id] [:td "&nbsp;"]]
                    [:tr [:th "Jméno"] [:td (:Label floor-info)] [:td "&nbsp;"]]
                    [:tr [:th "AOID"] [:td (:AOID floor-info)] [:td "&nbsp;"]]]
                [:h4 "Místnosti"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "ID"]
                         [:th "Jméno"]
                         [:th "AOID"]
                         [:th "Vytvořeno"]
                         [:th "Modifikováno"]
                         [:th "Platnost od"]
                         [:th "Platnost do"]
                         [:th "Kapacita"]
                         [:th "Typ"]
                         [:th "Obsazení"]
                         [:th "Interní/externí"]
                         [:th "Plocha"]
                         [:th ""]]
                    (for [room rooms]
                            [:tr [:td (:id room)]
                                 [:td (:name room)]
                                 [:td [:a {:href (sap-href room)} (:aoid room)]]
                                 [:td (:created room)]
                                 [:td (:modified room)]
                                 [:td (:valid_from room)]
                                 [:td (:valid_to room)]
                                 [:td (:capacity room)]
                                 [:td (:room_type_str room)]
                                 [:td (:occupied_by room)]
                                 [:td (if (= (:occupation room) "I") "interní" "externí")]
                                 [:td (:area room) "m<sup>2</sup>"]
                            ])
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))



(defn render-drawing-list
    "Render page with list of drawings."
    [project-id building-id floor-id project-info building-info floor-info valid-from drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 (str "Areál '" (:Label project-info) "', budova '" (get-building-name building-info) "' a podlaží '" (:Label floor-info) "'")]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr
                        [:th "Areál"]  [:td (:Label project-info)]
                        [:th "AOID"]   [:td (:AOID project-info)]
                        [:td [:a {:title "Podrobnější informace o areálu"
                                  :href (str "areal-info?areal-id=" project-id "&valid-from=" valid-from)}
                                  [:img {:src "icons/info.gif"}]]]]
                    [:tr
                        [:th "Budova"] [:td (get-building-name building-info)]
                        [:th "AOID"]   [:td (:AOID building-info)]
                        [:td [:a {:title "Podrobnější informace o budově"
                                  :href (str "building-info?areal-id=" project-id "&building-id=" building-id "&valid-from=" valid-from)}
                                  [:img {:src "icons/info.gif"}]]]]
                    [:tr
                        [:th "Podlaží"] [:td (:Label floor-info)]
                        [:th "AOID"]   [:td (:AOID floor-info)]
                        [:td [:a {:title "Podrobnější informace o podlaží"
                                  :href (str "floor-info?areal-id=" project-id "&building-id=" building-id "&floor-id=" floor-id "&valid-from=" valid-from)}
                                  [:img {:src "icons/info.gif"}]]]]
                    [:tr [:td {:colspan 5} "&nbsp;"]]
                    [:tr [:td {:colspan 2} "Zadaná platnost od:"]
                         [:td {:colspan 2} valid-from]
                         [:td [:a {:href "/help_valid_from"} [:img {:src "icons/help.gif"}]]]]
                ]
                [:br]
                [:h3 "Seznam výkresů"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Platnost od"] [:th "&nbsp;"]]
                    (for [drawing drawings]
                            [:tr [:td [:a {:href (str "drawing?areal-id=" project-id "&building-id=" building-id "&floor-id=" floor-id "&valid-from=" valid-from "&drawing-id=" (drawing-utils/filename->drawing-id drawing))} (drawing-utils/filename->drawing-version drawing floor-id)]]
                                 [:td [:a {:title "Podrobnější informace o výkresu"
                                           :href (str "drawing-info?drawing-id=" (drawing-utils/filename->drawing-id drawing))}
                                           [:img {:src "icons/info.gif"}]]]])
                ]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-drawing-list-from-sap
    "Render page with list of drawings."
    [floor-id drawings]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "#")
                [:h1 "Výběr výkresu"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr
                        [:th "Podlaží"] [:td "&nbsp;"]
                        [:th "AOID"]    [:td floor-id]
                    ]
                ]
                [:br]
                [:h3 "Seznam výkresů"]
                [:table {:class "table table-stripped table-hover" :style "width:auto"}
                    [:tr [:th "Platnost od"] [:th "&nbsp;"]]
                    (for [drawing drawings]
                            [:tr [:td [:a {:href (str "drawing-from-sap?floor-id=" floor-id "&valid-from=" (drawing-utils/filename->valid-from drawing floor-id) "&drawing-id=" (drawing-utils/filename->drawing-id drawing))} (drawing-utils/filename->drawing-version drawing floor-id)]]
                                 [:td [:a {:title "Podrobnější informace o výkresu"
                                           :href (str "drawing-info?drawing-id=" (drawing-utils/filename->drawing-id drawing))}
                                           [:img {:src "icons/info.gif"}]]]])
                ]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-floor-info-header
    "Render header for 'Vybrane podlazi'/'Selected floor'."
    []
    [:h4 [:a {:href "#" :onclick "showHideRoomInfo()" :title "Zobrazit či skrýt informace o vybraném podlaží"}
             [:img {:src "icons/1downarrow.gif" :id "show_hide_room_info"}]
             " Vybrané podlaží"]])


(defn render-floor-info-table
    "Render info for 'Vybrane podlazi'/'Selected floor'."
    [project-id building-id floor-id drawing-id project-info building-info floor-info drawing-info valid-from]
    [:table {:id "room_info" :class "table table-stripped table-hover" :style "width:auto;"}
        [:tr {:class "vcell"}
            [:th "Areál"]  [:td (:Label project-info)]
            [:th "AOID"]   [:td (:AOID project-info)]
            [:td [:a {:title "Podrobnější informace o areálu"
                      :href (str "areal-info?areal-id=" project-id "&valid-from=" valid-from)}
                      [:img {:src "icons/info.gif"}]]]
            [:td [:a {:title "Vybrat jiný areál"
                      :href (str "/areals?valid-from=" valid-from)}
                      [:img {:src "icons/view-list-tree.png"}]]]]
        [:tr {:class "vcell"}
            [:th "Budova"] [:td (:Label building-info)]
            [:th "AOID"]   [:td (:AOID building-info)]
            [:td [:a {:title "Podrobnější informace o budově"
                      :href (str "building-info?areal-id=" project-id "&building-id=" building-id "&valid-from=" valid-from)}
                      [:img {:src "icons/info.gif"}]]]
            [:td [:a {:title "Vybrat jinou budovu"
                      :href (str "/areal?areal-id=" project-id "&valid-from=" valid-from)}
                      [:img {:src "icons/view-list-tree.png"}]]]]
        [:tr {:class "vcell"}
            [:th "Podlaží"] [:td (:Label floor-info)]
            [:th "AOID"]   [:td (:AOID floor-info)]
            [:td [:a {:title "Podrobnější informace o podlaží"
                      :href (str "floor-info?areal-id=" project-id "&building-id=" building-id "&floor-id=" floor-id "&valid-from=" valid-from)}
                      [:img {:src "icons/info.gif"}]]]
            [:td [:a {:title "Vybrat jiné podlaží"
                      :href (str "/building?areal-id=" project-id "&building-id=" building-id "&valid-from=" valid-from)}
                      [:img {:src "icons/view-list-tree.png"}]]]]
        [:tr {:class "vcell"}
            [:th "Výkres"] [:td {:colspan 3} drawing-id]
            [:td [:a {:title "Podrobnější informace o výkresu"
                      :href (str "drawing-info?drawing-id=" drawing-id)}
                      [:img {:src "icons/info.gif"}]]]
            [:td [:a {:title "Vybrat jiný výkres"
                      :href (str "/floor?areal-id=" project-id "&building-id=" building-id "&floor-id=" floor-id "&valid-from=" valid-from)}
                      [:img {:src "icons/view-list-tree.png"}]]]]
    ])


(defn render-room-list-header
    "Render header for list of rooms."
    []
    [:h4 [:a {:href "#" :onclick "showHideRoomList()" :title "Zobrazit či skrýt informace o místnostech"}
             [:img {:src "icons/1downarrow.gif" :id "show_hide_room_list"}]
             " Seznam místností"]])


(defn render-room-list
    "Render list of rooms."
    [rooms]
    [:table {:id "room_list" :class "table table-stripped table-hover" :style "width:auto;"}
        [:tr {:class "vcell"}
             [:th "Jméno"]
             [:th "AOID"]
             [:th {:id "room_attribute_label"} ""]
             ;[:th "Platnost<br>od/do"]
             ;[:th "Typ"]
             ;[:th "Kapacita/<br>plocha"]
             ;[:th "Obsazení"]
        ]
        (for [room rooms]
                [:tr {:class "vcell"} [:td (:Label room)]
                     [:td [:a {:href "#" :onclick (str "onRoomSelect('" (:AOID room) "')")} (:AOID room)]]
                     [:td {:style "white-space: pre" :id (str "room_" (:AOID room) "_attribute_value")} ""]
                     ;[:td (:valid_from room) "<br>"
                     ;     (:valid_to room)]
                     ;[:td (:room_type_str room)]
                     ;[:td (:capacity room) "<br>"
                     ;     (:area room) "m<sup>2</sup>"]
                     ;[:td (:occupied_by room) "<br>"
                     ;     (if (= (:occupation room) "I") "interní" "externí")]
                ])
        ; not needed, let's use debugger instead for this old part of code
        ; [:tr [:td "Drawing:"] [:td [:div {:id "drawing-id"} drawing-id]]]
        ; [:tr [:td "Scale:"] [:td [:div {:id "scale"} "1"]]]
        ; [:tr [:td "X-pos:"] [:td [:div {:id "xpos"} "0"]]]
        ; [:tr [:td "Y-pos:"] [:td [:div {:id "ypos"} "0"]]]
    ])


(defn render-date-field
    "Render date field."
    [floor-id valid-from]
    (form/form-to {:name "inputForm"} [:get "/select-drawing-from-sap"]
        [:script "new tcal ({'formname': 'inputForm', 'controlname': 'valid-from'});"]
        [:input {:type "hidden" :name "floor-id" :id "floor-id" :value floor-id}]
        [:input {:type "text" :id "valid-from" :readonly "readonly" :name "valid-from" :value valid-from :style "width:7em;vertical-align:top;border:none;font-size:90%;text-align:center;"}]
        [:button {:type "submit" :class "btn btn-info btn-xs" :style "width:10em;vertical-align:top;"} "Změna platnosti"]
    ))

(defn render-filters-header
    "Render header for room filters part."
    []
    [:h4 [:a {:href "#" :onclick "showHideFilters()" :title "Zobrazit či skrýt informace o filtrech"}
             [:img {:src "icons/1downarrow.gif" :id "show_hide_filters"}]
             " Filtry"]])


(defn render-filters
    "Render room filters part."
    [project-id building-id floor-id valid-from room-attribute-types]
    [:table {:id "filters" :class ""}
        [:tr
            [:td {:style "vertical-align:top"}
                [:table {:id "filter-list" :class "table table-stripped table-hover" :style "width:auto;"}
                    (for [attribute room-attribute-types]
                        [:tr {:class "vcell"} [:td [:a {:href "#" :onclick (str "onAttributeTypeClicked('" (:ID attribute) "', '" (:Atribut attribute) "' ,'" floor-id "', '" valid-from "')")} (:Atribut attribute)]]]
                        )]]
            [:td {:style "width:1em"} "&nbsp;"]
            [:td {:style "vertical-align:top"}
                 [:div "Legenda"]
                 [:span {:id "legenda"}]
            ]]])
       ;[:tr {:class "vcell"}
       ;    [:td "Typ"]
       ;    [:td (form/check-box {:onclick "roomTypeCheckBoxClicked();"} "room-type-checkbox")]
       ;    [:td (passive-color-box "rgb(200,150,100)") "Kancelář" [:br]
       ;         (passive-color-box "rgb(100,150,200)") "Chodba" [:br]
       ;         (passive-color-box "rgb(200,140,200)") "Hala" [:br]
       ;         (passive-color-box "rgb(100,200,200)") "WC" [:br]
       ;         (passive-color-box "rgb(200,200,100)") "Technická místnost" [:br]
       ;         ]
       ;[:tr {:class "vcell"}
       ;    [:td "Kapacita"]
       ;    [:td (form/check-box {:onclick "roomCapacityCheckBoxClicked();"} "room-capacity-checkbox")]
       ;    [:td (passive-color-box "rgb(50,50,50)") "0" [:br]
       ;         (passive-color-box "rgb(100,100,100)") "1" [:br]
       ;         (passive-color-box "rgb(150,150,150)") "2" [:br]
       ;         (passive-color-box "rgb(200,200,200)") "&gt;2" [:br]
       ;    ]
       ;]
       ;[:tr {:class "vcell"}
       ;    [:td "Obsazení"]
       ;    [:td (form/check-box {:onclick "roomOccupationCheckBoxClicked();"} "room-ocupation-checkbox")]
       ;    [:td (passive-color-box "rgb(200,100,100)") "interní" [:br]
       ;         (passive-color-box "rgb(100,100,200)") "externí" [:br]
       ;    ]
       ;]
       ;[:tr {:class "vcell"}
       ;    [:td "Nájemce"]
       ;    [:td (form/check-box {:onclick "roomOccupiedByCheckBoxClicked();"} "room-occupied-by-checkbox")]
       ;    ]
       ;]])


(defn render-view-tools
    "Render all view tools with icons and help."
    [sap?]
    [:td {:class "tools" :background (if sap? "" "icons/elevel-logo-small.png")}
         [:span {:class "tools-spacer"}]
         [:a {:href "#" :title "Zvětšit"          :onclick "onViewMagPlusClick()"}  [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/viewmag_plus.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Zmenšit"          :onclick "onViewMagMinusClick()"} [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/viewmag_minus.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Původní měřítko"  :onclick "onViewMag11Click()"}    [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/viewmag_1_1.gif"}]] "&nbsp;"
         ;[:img {:src "icons/viewmag_fit.gif"    :border "0" :onclick "onViewMagFitClick()"}] "&nbsp;"
         [:span {:class "tools-spacer"}]
         [:a {:href "#" :title "Posunout doleva"  :onclick "onArrowLeftClick()"}    [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/arrow1l.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Posunout dolů"    :onclick "onArrowDownClick()"}    [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/arrow1d.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Posunout nahoru"  :onclick "onArrowUpClick()"}      [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/arrow1u.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Posunout doprava" :onclick "onArrowRightClick()"}   [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/arrow1r.gif"}]] "&nbsp;"
         [:a {:href "#" :title "Vycentrovat"      :onclick "onCenterViewClick()"}   [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/center.gif"}]] "&nbsp;"
         [:span {:class "tools-spacer"}]
         [:a {:href "#" :title "Zvýraznit okraje" :onclick "onViewBoundaryClick()"} [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/view_boundary.png"}]] "&nbsp;"
         [:a {:href "#" :title "Zobrazit mřížku"  :onclick "onViewGridClick()"}     [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/view_grid.png"}]] "&nbsp;"
         [:a {:href "#" :title "Zobrazit bod výběru"  :onclick "onViewBlip()"}      [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/view_blip.png"}]] "&nbsp;"
         [:a {:href "#" :title "Zobrazit kóty"  :onclick "onViewDimensionsClick()"} [:img {:style "margin:0px;padding:0px;border:none;" :src "icons/view_dimensions.png"}]]
         [:span {:class "tools-spacer"}]
    ])


(defn render-drawing
    "Render page with drawing on the right side and with configurable toolbar on the left side."
    [configuration project-id building-id floor-id drawing-id project-info building-info floor-info drawing-info valid-from valid-from-fmt rooms room-attribute-types sap?]
    (page/xhtml
        (if sap?
            (widgets/header "/" {:include-drawing-js? true
                                 :floor-id floor-id
                                 :raster-drawing-id drawing-id
                                 :include-calendar? true
                                 :sap-enabled (and (-> configuration :sap-interface :enabled) sap?)
                                 :sap-url     (-> configuration :sap-interface :url)})
            (widgets/header "/" {:include-drawing-js? true
                                 :floor-id floor-id
                                 :raster-drawing-id drawing-id
                                 :sap-enabled (and (-> configuration :sap-interface :enabled) sap?)
                                 :sap-url     (-> configuration :sap-interface :url)}))
        [:body (if sap? {:class "body-drawing-sap"} {:class "body-drawing"})
            (if sap?
                "" ;(widgets/navigation-bar "#")
                (widgets/navigation-bar "/"))
            [:table {:border "1" :style "border-color:#d0d0d0"}
                ; 1st row - the whole left toolbar + view tools on the right side
                [:tr
                    [:td {:rowspan 2 :style (if sap? "vertical-align:top;width:30em;" "vertical-align:top;width:35em;" )}
                        (if (not sap?)
                            [:span
                                (render-floor-info-header)
                                (render-floor-info-table project-id building-id floor-id drawing-id project-info building-info floor-info drawing-info valid-from)
                                (render-filters-header)
                                (render-filters project-id building-id floor-id valid-from room-attribute-types)
                                (render-room-list-header)
                                (render-room-list rooms)]
                            [:span
                                (render-date-field floor-id valid-from-fmt)
                                (render-filters-header)
                                (render-filters project-id building-id floor-id valid-from room-attribute-types)
                                (render-room-list-header)
                                (render-room-list rooms)])
                    ]
                    (render-view-tools sap?)
                ; 2nd row - drawing on the right side
                [:tr [:td {:style "vertical-align:top"} [:div {:style "position:relative;"} [:img {:id "drawing"
                                 :src (str "/raster-drawing?drawing-id=" drawing-id "&floor-id=" floor-id "&ignore-type=true")
                                 :border "0"
                                 :onclick "onImageClick(this, event)"}]]
                                 ; [:div {:id "sap_href_div" :style "display:none"} "&nbsp;"]
                                 [:div [:strong "Vybraná místnost: "]
                                       [:a {:id "sap_href" :name "sap_href"} [:span {:id "selected_room"} "?"]]]
                                 [:div {:style "height:100ex"} "&nbsp;"]]]
                ;[:tr [:td [:div {:class "canvas" :id "drawing_canvas"}]]]
            ]] ; </tr> </table>
            (if (not sap?)
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"])
            (widgets/footer)
        ] ; </body>
))


(defn render-raster-preview
    "Render page with preview of selected drawing.
    The drawing remains static and it won't be possible to click on rooms."
    [drawing-name]
    (page/xhtml
        (widgets/header "/")
        [:body {:class "body-drawing"}
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:img {:id "drawing" :src (str "/raster-drawing?drawing-name=" drawing-name) }]
                [:br]
                [:br]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-error-page
    "Render an error page with a 'back' button."
    [message]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Chyba či neočekávaný stav"]
                [:p {:class "alert alert-danger"} message]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                [:br][:br][:br][:br]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-error-page-sap
    "Render an error page without a 'back' button."
    [message]
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Chyba či neočekávaný stav"]
                [:p {:class "alert alert-danger"} message]
                [:br][:br][:br][:br]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))


(defn render-store-settings-page
    "Render page with setting dialog and a 'back' button."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nastavení"]
                [:p {:class "alert alert-success"} "Změny byly úspěšně uloženy"]
                [:button {:class "btn btn-primary" :onclick "window.history.back()" :type "button"} "Zpět"]
                [:br][:br][:br][:br]
                (widgets/footer)
            ] ; </div class="container">
        ] ; </body>
))

