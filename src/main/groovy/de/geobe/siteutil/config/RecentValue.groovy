/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.siteutil.config

import de.geobe.siteutil.main.SiteUtil
import groovy.json.JsonOutput
import groovy.json.JsonParserType
import groovy.json.JsonSlurper

import java.time.LocalDateTime

/**
 * Keep a persistent record of the last dynIP and certificate update
 * to avoid unneccessary interactions with the DNS and ACME servers
 * Created by georg beier on 04.05.2020.
 */
class RecentValue {

    static final SITEUTIL_DIR = 'siteutil'
    static final SITEUTIL_FILE = 'recent.json'

    /** the RecentValue singelton */
    private static RecentValue theValue

    /** last dynamic ip that was set on the dns server */
    String lastIp = ''
    /** timestamp of last ip update */
    Date ipSetAt //= new Date()
    /** timestamp of last certificate challenge update */
    Date certificateReceived //= new Date()
    /** timestamp for next certificate update action */
    Date certificateExpires //= new Date() + 10

    private RecentValue() {}

    /**
     * access to the singleton value
     */
    static RecentValue getValue() {
        if (!theValue) {
            theValue = restoreOrInit()
        }
        theValue
    }

    /**
     * Set the singleton from a file at first access with
     * values saved on an earlier run
     * @return initialised object or empty object
     */
    private static restoreOrInit() {
        def home = System.getProperty('user.home')
        def file = new File("$home/.$SITEUTIL_DIR/", "$SITEUTIL_FILE")
        if (file.exists() && file.text) {
            def json = file.text
            fromJson(json)
        } else {
            new RecentValue()
        }
    }

    /**
     * Save singleton to a file in json format
     * @return
     */
    def save() {
        def home = System.getProperty('user.home')
        def valueDir = "$home/.$SITEUTIL_DIR/"
        def dir = new File(valueDir)
        if (!dir.exists()) {
            dir.mkdir()
        }
        if (dir.isDirectory()) {
            def json = toJson()
            new File(valueDir, SITEUTIL_FILE).withWriter { w ->
                w << json
            }
        }
    }

    /**
     * parse json string to a RecentValue object
     * @param json
     * @return
     */
    private static fromJson(json) {
        def slurper = new JsonSlurper()
        // only LAX parser converts date strings to dates
        slurper.type = JsonParserType.LAX
        def read = slurper.parseText(json) as RecentValue
        read
    }

    /**
     * convert RecentValue object to json string
     * @return
     */
    private toJson() {
        def valMap = [lastIp             : lastIp,
                      ipSetAt            : ipSetAt,
                      certificateReceived: certificateReceived,
                      certificateExpires : certificateExpires]
        def jsonValue = JsonOutput.toJson(valMap)
        def out = JsonOutput.prettyPrint(jsonValue)
        out
    }

    public static void main(String[] args) {
        def rv = RecentValue.value
        println "last ip: ${rv.lastIp}, set at ${rv.ipSetAt}"
        if(!rv.lastIp) {
            rv.lastIp = '192.168.168.192'
            rv.ipSetAt = new Date()
            rv.save()
        }
    }
}

