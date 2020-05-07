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

package de.geobe.siteutil.dns

import geb.spock.GebSpec
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Shared

import java.time.LocalTime

/**
 * Created by georg beier on 10.03.2020.
 */
class SiteInteractionSpec extends GebSpec {

    @Shared config = new IspDnsConfig('liveConfig.cfg')
    @Shared mockKey = 'hurzburz' + LocalTime.now()
    @Shared fieldId = ''
    static String browserBaseUrl, logoutUrl
    def usr = config.login.user
    def pwd = config.login.password
    def out = config.logout.url
    def edit = config.dyndns.url
    def targetName = config.dyndomain
    def acmeChallenge = config.acmeChallenge
    def targetIp = '192.168.168.192'

    def setupSpec() {
        LoginPage.url = config.login.url
        LoginPage.at = { title == config.login.title }
        LogoutPage.url = config.logout.url
        LogoutPage.at = { title == config.login.title }
        AdminPage.url = config.admin.url
        AdminPage.at = { title == config.admin.title }
        DnsEditPage.url = config.dyndns.url
        DnsEditPage.at = { title == config.dyndns.title }
    }

    @Ignore
    def 'can find elements on login page'() {
        when:
        to LoginPage
        sleep(100)
        then:
        loginForm.userField
        loginForm.passwordField
        loginForm.commitButton
    }

    @Ignore
    def 'can login and logout'() {
        when:
        to LoginPage
        println "at page: $page title: $title url: $pageUrl"
        login(usr, pwd)
        at AdminPage
        println "at page: $page title: $title url: $pageUrl"
        to LogoutPage
        println "at page: $page title: $title url: $pageUrl"
        then:
        sleep(2000)
    }

    @Ignore
    def 'can go to dns config page'() {
        when:
        to LoginPage
        login(usr, pwd)
        println logged.text()
        println "go to url $edit"
        to DnsEditPage
        sleep(2000)
        then:
        at DnsEditPage
        cleanup:
        go out
        sleep(2000)

    }

//    @Ignore
    def 'can find relevant inputs on dns config page'() {
        when:
        to LoginPage
        login(usr, pwd)
        to DnsEditPage
        def found1 = setRecordForDomain(acmeChallenge.nameFieldValue, 'TXT', mockKey)
        println "$found1 -> dataRecordId = ${found1.dataRecordId}"
        fieldId = found1.dataRecordId
        println "new value: ${$('form', id: 'form-dns').$('input', name: 'content_' + found1.dataRecordId).value()}"
        sleep(2000)
        then:
        at DnsEditPage

        cleanup:
        go out
        sleep(2000)

    }


//    @Ignore
    def 'configuration was done by methods'() {
        when:
        to LoginPage
        login(usr, pwd)
        to DnsEditPage
        def actualValue = $('form', id: 'form-dns').
                $('input', name: 'content_' + fieldId).value()
        sleep(2000)
        then:
        actualValue == mockKey
        cleanup:
        go out
        sleep(2000)
    }

}
