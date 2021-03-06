/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.system.controller.LoggingController
import org.simplemes.eframe.web.ui.webix.DomainToolkitUtils

import java.text.SimpleDateFormat

/**
 * Provides the efGUISetup freemarker implementation.
 * This extracts the current request's locale and includes the correct language files.
 */
class GUISetupMarker extends BaseMarker {

  @Override
  void execute() {
    def locale = GlobalUtils.getRequestLocale() ?: Locale.default
    def sb = new StringBuilder()
    def toolkitLang = getToolkitDialectForLocale(locale)

    sb << """<script type="text/javascript">\n"""
    sb << """  webix.i18n.setLocale("${toolkitLang}");\n"""
    sb << """  webix.i18n.fullDateFormat = "${getFullDateFormat(locale)}";\n"""
    sb << """  webix.i18n.dateFormat = "${getDateFormat(locale)}";\n"""
    sb << """  webix.i18n.parseFormat = "%Y-%m-%d %H:%i:%s";\n"""
    sb << """  webix.i18n.setLocale();\n"""
    sb << buildLoggerSetup()
    sb << """</script>\n"""

    write(sb.toString())
    //body?.render(environment.getOut())
  }

  /**
   * The toolkit-specific language/dialect needed for the given language.
   */
  static toolkitLanguages = [en: "en-US",
                             ru: "ru-RU",
                             fr: "fr-FR",
                             ja: "ja-JP",
                             be: "be-BY",
                             de: "de-DE",
                             es: "es-ES",
                             it: "it-IT",
                             zh: "zh-CN",
                             pt: "pt-BR"]

  /**
   * Gets the full language/dialect for the given locale that works for the toolkit.
   * @param locale The locale.
   * @return The lang/dialect (e.g. 'en-US') in toolkit format.
   */
  String getToolkitDialectForLocale(Locale locale) {
    def lang = locale?.language ?: 'en'
    def s = toolkitLanguages[lang]
    if (!s) {
      s = "${locale?.language}-${locale?.country?.toUpperCase()}"
    }
    return s
  }

  /**
   * Gets the full date/time format for the given locale.
   * @param locale The locale.
   * @return The toolkit full date format.
   */
  String getFullDateFormat(Locale locale) {
    return DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateFormat(locale))
  }

  /**
   * Gets the date only format for the given locale.
   * @param locale The locale.
   * @return The toolkit date format.
   */
  String getDateFormat(Locale locale) {
    return DomainToolkitUtils.instance.convertDateFormatToToolkit((SimpleDateFormat) DateUtils.getDateOnlyFormat(locale))
  }

  /**
   * Builds the javascript logger setup for this page.
   * @returns The javascript to initialize the logger.
   */
  String buildLoggerSetup() {
    // Find the effective logging level for the page.
    def page = markerContext?.uri
    if (!page) {
      return ''
    }
    def loggerName = ControllerUtils.instance.determineBaseURI(LogUtils.convertPageToClientLoggerName(page))
    def level = LogUtils.getLogger(loggerName).effectiveLevel
    def clientLevel = LogUtils.convertLevelToClientLogSetting(level)
    def url = "/logging/client?logger=${loggerName}"

    def toServerClientLevel = 4000
    def toServerLevel = LogUtils.getLogger(LoggingController.CLIENT_TO_SERVER_LOGGER).level
    if (toServerLevel) {
      toServerClientLevel = LogUtils.convertLevelToClientLogSetting(toServerLevel)
    }

    return """
        JL().setOptions({"level": ${clientLevel},
                         "appenders": [JL.createAjaxAppender('ajaxAppender').setOptions({"url": "${
      url
    }", "level": $toServerClientLevel}),
                          JL.createConsoleAppender('consoleAppender')]});
    """

  }

  // TODO: Support theme.
}
