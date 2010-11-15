import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import org.codehaus.groovy.grails.commons.ConfigurationHolder

includeTargets << grailsScript("_GrailsArgParsing")

USAGE = """
    autotranslate LANGUAGES [Overwrite]

where
    LANGUAGES  = Comma seperated list of language codes to convert to, see website for details
	Overwrite = Should current translations be replaced. Default false. Useful to not overwrite any hand tuned translations
"""

target(main: "Translates base properties files into configured languages") {	
	def (languages, overwrite) = parseArgs()
		
    Translate.setHttpReferrer("http://${InetAddress.getLocalHost().toString()}");

	def enTranslations = []
	def i18nFiles = new File("${basedir}/grails-app/i18n")
	i18nFiles.eachFile {
		// Grab all the base english files - xyz.properties in grails land
		if(!it.name.contains('_') && it.name.endsWith('.properties')) {
			Properties prop = new Properties()
			FileInputStream inf = new FileInputStream(it)
			prop.load(inf)
			enTranslations.add(prop)
		}
	}
	
	Properties[] translations = new Properties[languages.size()]
	languages.eachWithIndex { lang, i ->
		def f = new File("${basedir}/grails-app/i18n/messages_${lang.toString()}.properties")
		if(!f.exists())
			f.createNewFile()
		
		Properties prop = new SortedProperties()
		FileInputStream inf = new FileInputStream(f)
		prop.load(inf)
		translations[i] = prop
	}
	
	enTranslations.each { baseline ->
		def keys = baseline.keys()
		keys.each { key ->
			def text = baseline.getProperty(key)
			if(text) {
				println "Translating: " + key
				def translatedText = Translate.execute(text, Language.ENGLISH, languages)
				translatedText?.eachWithIndex { translation, i ->
					if(!translations[i].containsKey(key) || overwrite)
						translations[i].put(key, translation)
				}
			}
		}
	}
	
	translations.eachWithIndex { prop, i ->
		FileOutputStream outf = new FileOutputStream(new File("${basedir}/grails-app/i18n/messages_${languages[i].toString()}.properties"))
		prop.store(outf, "Automatic tranlsation by Grails Autotranslate plugin")
	}
}

setDefaultTarget(main)

def parseArgs() {
	args = args ? args.split('\n') : []
	switch (args.size()) {
		case 1:
			println "Translating to languages ${args[0]}"
			return [parseLanguages(args[0]), false]
			break
		case 2:
			println "Translating to languages ${args[0]}"
			return [parseLanguages(args[0]), args[1] == "true"]
			break
		default:
			usage()
			break
	}
}

private Language[] parseLanguages(langs) {
	def languages = []
	langs.split(',').each {
		languages.add( Language.fromString(it) )
	}
	
	return languages.toArray(new Language[0])
}

private void usage() {
	println "Usage:\n${USAGE}"
	System.exit(1)
}

public class SortedProperties extends Properties {
  @Override
  public synchronized Enumeration keys() {
     def keysEnum = super.keys()
     def keyList = new Vector()
     while(keysEnum.hasMoreElements()){
		keyList.add(keysEnum.nextElement())
     }
     Collections.sort(keyList);
     keyList.elements();
  }
}
