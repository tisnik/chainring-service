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

(ns chainring-service.html-renderer-help
    "Module that contains functions used to render HTML pages with help that are sent back to the browser.

    Author: Pavel Tisnovsky")


(require '[hiccup.page  :as page])

(require '[chainring-service.html-renderer-widgets :as widgets])


(defn valid-from
    "Help page for the valid-from field."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Datum platnosti"]
                [:p "Stavební objekty v SAPu mají nastaven datum platnosti, podle kterého je možné určit časový úsek, kdy takový objekt reálně existuje."]
                [:p "Například při zbourání příčky v SAPu zanikne jedna z místností, což se projeví i na výkresech."]
                [:p "Implicitně je datum platnosti nastaveno na aktuální den."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn valid-from-settings
    "Help page for the valid-from settings."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Nastavení data platnosti"]
                [:p "Stavební objekty v SAPu mají nastaven datum platnosti, podle kterého je možné určit časový úsek, kdy takový objekt reálně existuje."]
                [:p "Například při zbourání příčky v SAPu zanikne jedna z místností, což se projeví i na výkresech."]
                [:p "Implicitně je datum platnosti nastaveno na aktuální den."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn intreno
    "Help page for AOID field."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Interní identifikátor objektu"]
                [:p "Interní identifikátor objektu používaný SAPem."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn aoid-areal
    "Help page for AOID field for areal."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "AOID Areálu"]
                [:p "Jedná se o jednotný identifikátor areálu používaný v SAPu. Tento identifikátor má tvar celého kladného čísla (1, 2, atd.)."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn name-areal
    "Help page for name field for areal."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Jméno areálu"]
                [:p "Jedná se o jméno areálu používané v SAPu. Tvar jména může být prakticky libovolný a není povinný."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn valid-from-areal
    "Help page for valid-from field for areal."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Začátek platnosti"]
                [:p "Začátek platnosti informací o areálu převzaný ze SAPu"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn valid-to-areal
    "Help page for valid-to field for areal."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Konec platnosti"]
                [:p "Konec platnosti informací o areálu převzaný ze SAPu"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn aoid-building
    "Help page for AOID field for building"
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "AOID Budovy"]
                [:p "Jedná se o jednotný identifikátor budovy používaný v SAPu. Tento identifikátor má tvar celého kladného čísla následovaného tečkou a identifikátorem budovy: A.A1 atd."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn name-building
    "Help page for name field for building."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Jméno budovy"]
                [:p "Jedná se o jméno budovy používané v SAPu. Tvar jména může být prakticky libovolný a není povinný."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn valid-from-building
    "Help page for valid-from field for building"
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Začátek platnosti"]
                [:p "Začátek platnosti informací o budově převzaný ze SAPu"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn valid-to-building
    "Help page for valid-to field for building"
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Konec platnosti"]
                [:p "Konec platnosti informací o budově převzaný ze SAPu"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn floor-count-building
    "Help page for floor-count field."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Počet podlaží"]
                [:p "Zjištěný počet podlaží v budově"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn aoid-floor
    "Help page for AOID field for floor."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "AOID Podlaží"]
                [:p "Jedná se o jednotný identifikátor podlaží používaný v SAPu. Tento identifikátor má tvar celého kladného čísla následovaného tečkou a identifikátorem budovy a identifikátorem podlaží: 1.A1.1MP atd."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn name-floor
    "Help page for name field for floor."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Jméno podlaží"]
                [:p "Jedná se o jméno podlaží používané v SAPu. Tvar jména může být prakticky libovolný a není povinný."]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))


(defn drawing-count-floor
    "Help page for floor-count floor."
    []
    (page/xhtml
        (widgets/header "/")
        [:body
            [:div {:class "container"}
                (widgets/navigation-bar "/")
                [:h1 "Nápověda"]
                [:h3 "Počet výkresů"]
                [:p "Zjištěný počet výkresů pro podlaží v dané oblasti platnosti"]
                [:br]
                (widgets/back-button)
                [:br][:br][:br][:br]
                (widgets/footer)
            ]]))

