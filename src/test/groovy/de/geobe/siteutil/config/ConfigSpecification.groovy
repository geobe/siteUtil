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

import spock.lang.Specification

/**
 * Created by georg beier on 08.03.2020.
 */
class ConfigSpecification extends Specification {
    def config = new Configuration()

    def "read simple values from testConfiguration"() {
        when: 'configuration is initialised'
        config.init()
        then: 'all values are available'
//        conf.get('login.')
        config.conf.basedomain == 'myhome.io'
        config.conf.dyndomain == 'at.' + config.conf.basedomain
        config.conf.adminbase == 'https://dns.example.com/'
    }

    def "read nested values"(){
        when: 'configuration is initialised'
        def conf = config.init()
        def dnssite = config.conf.dnssite
        then: 'nested values are available'
        dnssite.login.user == 'rubberduck'
    }

}
