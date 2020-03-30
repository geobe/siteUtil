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
        def conf = new Configuration().init(configfile)
        login = conf.dnssite.login
        admin = conf.dnssite.admin
        dyndns = conf.dnssite.dyndns
        acmeChallenge = conf.dyndns.acmeChallenge
        logout = conf.dnssite.logout
        baseUrl = conf.dnssite.baseUrl
        dyndomain = conf.dyndomain
    }

    def setIpAddress(String myIp) {
        setDnsRecord('A', myIp)
    }

    def setTxtRecord(String acmeChallenge) {
        setDnsRecord('TXT', acmeChallenge)
    }

    private setDnsRecord(String recType, String recValue) {
        def retval
        Browser.drive {
            to LoginPage
            login(login.user, login.password)
            to DnsEditPage
            retval = setRecordForDomain(dyndomain, recType, recValue)
            go logout.url
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