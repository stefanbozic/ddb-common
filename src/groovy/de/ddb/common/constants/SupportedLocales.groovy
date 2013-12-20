/*
 * Copyright (C) 2013 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.common.constants;


public enum SupportedLocales {

    EN(Locale.US, 0),
    DE(Locale.GERMANY, 1)

    private Locale locale
    private int priority

    SupportedLocales(Locale locale, int priority) {
        this.locale = locale
        this.priority = priority
    }

    public static Locale getDefaultLocale() {
        return SupportedLocales.EN.locale
    }

    public String getISO2() {
        return this.locale.getLanguage();
    }

    public String getISO3() {
        return this.locale.getISO3Language();
    }

    public Locale getLocale() {
        return locale
    }

    public static List<Locale> getSupportedLocales() {
        SupportedLocales[] supported = SupportedLocales.values();
        ArrayList<Locale> out = new ArrayList<Locale>()
        for (SupportedLocales support : supported) {
            out.add(support.locale)
        }
        return out
    }

    public static List<String> getSupportedLanguagesISO2() {
        SupportedLocales[] supported = SupportedLocales.values()
        ArrayList<String> out = new ArrayList<String>()
        for (SupportedLocales support : supported) {
            out.add(support.locale.getLanguage())
        }
        return out
    }

    public static List<String> getSupportedLanguagesISO3() {
        SupportedLocales[] supported = SupportedLocales.values()
        ArrayList<String> out = new ArrayList<String>()
        for (SupportedLocales support : supported) {
            out.add(support.locale.getISO3Language())
        }
        return out
    }


    public static List<SupportedLocales> getSupportedLocalesByPriority(topPriorityLocale) {
        SupportedLocales[] supported = SupportedLocales.values()
        ArrayList<SupportedLocales> out = new ArrayList<SupportedLocales>()
        for (SupportedLocales support : supported) {
            out.add(support)
        }
        Collections.sort(out)
        for (int i=0; i < out.size(); i++) {
            def entry = out.get(i)
            if(entry.getISO2() == topPriorityLocale.getLanguage()){
                out.remove(i)
                out.add(0, entry)
                break
            }
        }
        return out
    }


    public static boolean supports(Locale locale) {
        if (locale == null) {
            return false
        }
        String language = locale.getLanguage()
        SupportedLocales[] supported = SupportedLocales.values()
        for (SupportedLocales support : supported) {
            if (support.locale.getLanguage().equals(language)) {
                return true
            }
        }
        return false
    }

    public static Locale getBestMatchingLocale(Locale input){
        Locale locale = input
        if(!locale){
            locale = SupportedLocales.getDefaultLocale()
        }
        if(!SupportedLocales.supports(locale)){
            locale = SupportedLocales.getDefaultLocale()
        }
        SupportedLocales[] supported = SupportedLocales.values()
        for(int i=0; i<supported.length; i++){
            if(supported[i].getISO2().equals(locale.getLanguage())){
                locale = supported[i].getLocale()
                break;
            }
        }
        return locale
    }

    public static Locale getBestMatchingLocale(String input){
        Locale locale = new Locale(input)
        return getBestMatchingLocale(locale)
    }

    public static Locale getDefinedLocale(Locale locale) {
        if (locale == null) {
            return null
        }
        String language = locale.getLanguage()
        SupportedLocales[] supported = SupportedLocales.values()
        for (SupportedLocales support : supported) {
            if (support.locale.getLanguage().equals(language)) {
                return support.locale
            }
        }
        return null
    }

    int compareTo(Object o){
        if(o instanceof SupportedLocales){
            if(this.priority > ((SupportedLocales) o).priority){
                return 1
            }else if(this.priority < ((SupportedLocales) o).priority){
                return -1
            }else{
                return 0
            }
        }else{
            return 0
        }
    }
}
