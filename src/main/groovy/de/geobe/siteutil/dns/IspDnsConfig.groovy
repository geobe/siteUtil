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

import de.geobe.siteutil.config.Configuration
import geb.Browser
import geb.Module
import geb.Page
import geb.module.PasswordInput
import geb.module.TextInput

/**
 * Created by georg beier on 07.03.2020.
 */
class IspDnsConfig {
    def waitOnPage = false
    def sleeptime = 1000
    def login
    def admin
    def dyndns
    def acmeChallenge
    def logout
    def baseUrl
    def dyndomain

    /**
     * Read values to access ISP DNS config web page from configuration file and expose them as variables
     * @param configfile
     */
    IspDnsConfig(String configfile) {
        def config = new Configuration().init(configfile)
        login = config.dnssite.login
        admin = config.dnssite.admin
        dyndns = config.dnssite.dyndns
        acmeChallenge = config.dnssite.acmeChallenge
        logout = config.dnssite.logout
        baseUrl = config.dnssite.baseUrl
        dyndomain = config.dyndomain

    }

    def initUris() {
        LoginPage.url = login.url
        LoginPage.at = { title == login.title }
        LogoutPage.url = logout.url
        LogoutPage.at = { title == login.title }
        AdminPage.url = admin.url
        AdminPage.at = { title == admin.title }
        DnsEditPage.url = dyndns.url
        DnsEditPage.at = { title == dyndns.title }

    }

    def setIpAddress(String myIp) {
        setDnsRecord(dyndns.nameFieldValue,'A', myIp)
    }

    def setTxtRecord(String acmeChallenge) {
        setDnsRecord(acmeChallenge.nameFieldValue,'TXT', acmeChallenge)
    }

    /**
     * Set a field value on the dns configuration page, @see DnsEditPage#setRecordForDomain
     * @param recordInputFieldName name attribute of html input
     * @param recType DNS record type, must be eiteher A or TXT
     * @param recValue value to be set
     * @return
     */
    private setDnsRecord(String recordInputFieldName, String recType, String recValue) {
        def retval
        Browser.drive {
            initUris()
            to LoginPage
            if(waitOnPage) Thread.sleep(sleeptime)
            login(login.user, login.password)
            if(waitOnPage) Thread.sleep(sleeptime)
            to DnsEditPage
            if(waitOnPage) Thread.sleep(sleeptime)
            retval = setRecordForDomain(recordInputFieldName, recType, recValue)
            if(waitOnPage) Thread.sleep(sleeptime)
            go logout.url
            if(waitOnPage) Thread.sleep(sleeptime)
        }
        return retval
    }
}

/**
 * Login web page
 */
class LoginPage extends Page {
    static url
    static at
    static atCheckWaiting = true

    static content = {
        userField { $("input", name: "username").module(TextInput) }
        passwordField { $("input", name: "password").module(PasswordInput) }
        commitButton(to: AdminPage) { $("input", type: "submit") }
    }

    void login(user, pw) {
        userField.text = user
        passwordField.text = pw
        commitButton.click()
    }

}

/**
 * Logout web page
 */
class LogoutPage extends Page {
    static url
    static at
    static atCheckWaiting = true

}

/**
 * Web page reached after login
 */
class AdminPage extends Page {
    static url
    static at
    static atCheckWaiting = true

    static content = {
        logged { $('p', 0, class: 'logged') }
    }
}

/**
 * Web page where the actual DNS configuration records can be edited
 */
class DnsEditPage extends Page {
    static url
    static at
    static atCheckWaiting = true

    /**
     * This method works with the DNS configuration page at internetwerk.de.
     * You will have to adapt it to your ISP administration page layout.
     * It allows to set the dynamic IP address of your home networks dns name
     * and to set a TXT record for verification of your ownership of the DNS entries as
     * required by ACME servers like LetsEncrypt.
     * @param targetDomain name of subdomain to configure
     * @param recordType 'A' for changing your dynDns, 'TXT' for LetsEncrypt verification entry
     * @param fieldValue 'A': your actual IP, 'TXT': string received from LetsEncrypt
     */
    def setRecordForDomain(String targetDomain, recordType, fieldValue) {
        // the table of fields within the input form (more than one at internetwerk)
        def table = $('form', id: 'form-dns').$('table')
        // table row to deal with
        def rows = table.find('tr', 'data-type': recordType).
                has('input', name: ~/name_.+/, value: targetDomain)
        assert rows.size() == 1
        def row = rows.first()
        def valueField = row.$('input', name: ~/content_.+/)
        def retval = [dataRecordId: row.attr('data-record-id'),
                      domainName  : row.$('input', name: ~/name_.+/).value(),
                      oldValue    : valueField.value()]
        valueField.value(fieldValue)
        def submit = $('form', id: 'form-dns').$('input', class: 'btn_save')
        submit.click()
        return retval
    }

}