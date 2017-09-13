/*
 * Modified in 2017 by Stratio Big Data Inc.,
 * Sucursal en España. Modifications are ©  2017
 * Stratio Big Data Inc.,Sucursal en España.
 */

package org.apache.spark.security

import java.nio.file.{Files, Paths}
import java.security.MessageDigest

import org.scalatest.ShouldMatchers
import scala.io.Source

import org.apache.spark.SparkFunSuite


class SSLConfigTest extends SparkFunSuite with ShouldMatchers{

  // scalastyle:off
  val pemString = "-----BEGIN CERTIFICATE-----MIIFXTCCA0WgAwIBAgIRALZoIdzosSCFgK8nHH+iKoQwDQYJKoZIhvcNAQELBQAwNjELMAkGA1UEBhMCRVMxEDAOBgNVBAoMB1N0cmF0aW8xFTATBgNVBAMMDExhYnMgdGVhbSBDQTAeFw0xNzA2MjAxNjQ0NDFaFw0xODA2MjAxNjQ0NDFaMDIxCzAJBgNVBAYTAkVTMRAwDgYDVQQKEwdTdHJhdGlvMREwDwYDVQQDEwhwb3N0Z3JlczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK5X78hm/LT6WVA3gswBE9/Z5qYovyD+aU2ZTMU89MB+VGzRfjwsO4Exg/AuRHj369mj/zNHCXcumWe06az6Q0Jj8Gv/u1vzA8LJMcyF3EhLSG8EiIp7LEXm+uSs1rkRH1qM2j2oVyIC4IUhrwvBWT3VnZFXTSnpodwARYGszkyrv+1poQTMLz5gvolv1kQWexB6zTaTIG4dpiGwyjtbtwlqa6LQe2KXq6C6eLUX/bPHPaU/ECdl/A1jHT8kyTe9FT3OSzsJCwPII/oMYoJDYFUBnLrBd4lKy00KgoopIzn+xtbLwq3nxsHPBbHStFkw8+2BoDGUDidAQPNiPJovC6cCAwEAAaOCAWgwggFkMHQGCCsGAQUFBwEBBGgwZjAyBggrBgEFBQcwAoYmaHR0cDovL2xhYnMtY2Euc3RyYXRpby5jb20vbGFicy1jYS5jcnQwMAYIKwYBBQUHMAGGJGh0dHA6Ly9vY3NwLmxhYnMtY2Euc3RyYXRpby5jb206OTA4MTAfBgNVHSMEGDAWgBQJKT+GZuaidBa59eIk7bYUgq11UjAMBgNVHRMBAf8EAjAAMDcGA1UdHwQwMC4wLKAqoCiGJmh0dHA6Ly9sYWJzLWNhLnN0cmF0aW8uY29tL2xhYnMtY2EuY3JsMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0OBBYEFO454se8dRSIP2jLPsYIC8B8wtrpMDYGA1UdEQQvMC2CCHBvc3RncmVzgiFwb29scGdzcWx0ZXN0c3BhcmsubWFyYXRob24ubWVzb3MwDQYJKoZIhvcNAQELBQADggIBAGm45ighjWkdj662m72AuPNIofTqQhn+Fkwwvc+KVk4s0wdLJTJvfQ7JaznCnFhIpowEW6J6hyF/C7ltMpjnPuhzbs0a12+I2HC5ZWOB8ag8zuv/+uYNUFiUzVQaLjKndwsOY60Tqy+nEGKTDXCWh6hz4+WideI60JB8DzPVzaEp74U+t0/grbiXXb08ft10AQO52QR49r67xRHC9GxY5YRZNe07uT12jWeWUGasUzNFOHAB/dFcLgkxaUMFeaBR40XlLZo/Y/TbxCsnvTe4uP7nn+PBYVSS8y+sTAQ+l7X6UiK0VPWF8hlURHhM9K6EbvNJFpkDBXRM66ZDV3uXN6Jyx+8qjPM6ekjofwJgTDPfiigSV27OFcq0OwqvO+7797e/MDQ0tItsShmTtpcEShaF5l8VM7laoCqLGObIyQJHkpIslGVU2qutJyMKwOrdKfbWJeJphgbpGuPeiwrJ8OSIkO+u1pPtDiLwm+QqilEz5VOAxb3Lco2BEp2GwTVW3zZtBuRWdYxWcIB0LvYYRGw0D1DaEAUf1Hr2jJeBUvQsNre962Tl9r5wGSqTAXQLykoxIAOzaN56Gzncrnt1iw2MQyUiN+9zisnE8FpjnSRekJCZljFkUxJARFSUah9mecYRVm9G8YkxCg3feUfvw0lQdqqw/kFkFFe3RC0agHj/-----END CERTIFICATE----------BEGIN CERTIFICATE-----MIIFOjCCAyKgAwIBAgIQdWSbQJ/CYk1/BERap1nTPTANBgkqhkiG9w0BAQsFADA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwHhcNMTYxMDIxMTUxNjEyWhcNMjYxMDE5MTUxNjEyWjA2MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEVMBMGA1UEAwwMTGFicyB0ZWFtIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtMw5w9bxjXur+T3A/sFEcDCKxvKWU1Um86puh36D3Zc55/aTpNzq8UqyEtTUbr9xuqpkriQWPZcS3CZEDW0wnEXmZ6/ukQ059T9wv6P+YGEXMqaVHn3qPGwiZ8WbClVrGfDwTl+9sfqvfR6keJq8rJkxE21ECW94ikI7Tk19s0Rz62xf+/FFRndQbTsech9Opi4TC2zMd9h9rPyRwfSmVHMKPmqm+nnAoDjBlUalxjt+n7Vs269ZBqfusn25Em+BIMwU4z13csHIuZuB/mvmqipxc2VHHrvhlCeoSgqWnNvmok4D8Ug+9sASAKYn1stdkSxCqwLLWb9PR/fUcqorvz1S8KNc07c7IIt2ip5sXGWSJKIazak4CHiJGEQ11jO4NOFsXp+tDaQgT/21Aq45zHJyf9ymT8c0ztwe9oQI1UYXHyopfyW1o9Ca5/MxQeXWcCPEFE1IMoA/e6vDWF09liDOZl94lSwZ2ev+zGGDN+WU4ZojCbjdLCv/tbuSdyiTd4jV6dCShObyWMc6l+TAfHvHFkxD29MDC165evoSXWMZtqriyt2h/9kWmqgblpz0oJ4kFPQ9RY5n34FHMSvabkSTWkjcV8m6/4YDaRWYTo7a+ObJ9a6sk0J8pfYYcpwn4QoFS+12IA95ea51TWN1bEoIFaVEqBDy5y3Qe7JSWNkCAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFAkpP4Zm5qJ0Frn14iTtthSCrXVSMA0GCSqGSIb3DQEBCwUAA4ICAQA/Pnv9Lf/Ic/EeJYjFGZ84bICIZ/cFFNPcb4lWc7zJNOg2690t748MwqDUQYvCL4f5Dh+rvssLxK/FlMGDiw4UrZGHdHS5u3bTTUrbwG1NHQKRnthdz1xtx/TlBn4NvqVokcB7AkC5XVwpTvvVvcgpRp+ffYK3gd6arOb6oOOzRYDawFgPEZdZrbREh9p1k0NpbkUcmNiNMjQhV+DqVvGI+pYTOP4686Q4PvC7OGma5X3T0MWwUJ3iMPdo4qAGJB8GV4XbyPapMd7vqFzsf18yAKmT9Q2afr1U1KCQ/zourY1uqBeor5CzJzCDqLDxgXrBauVwB844Epa24x98RQGfZuIULZGPn0Sf8sF4L2bwmBBQM29yAWO4DLEZU0yqQTUHV7oJmNN+i0iE4wNSlC/Gz/wV3LCG+tseQkgvRziN8hwoTGg6n9EvT2NdD0QpzJ2D7PQeJIytKEYzJKVRCZkn3Xcoy1SzIKiG0xDLME22rjFuvWFyw1r/OnErIM7RL4GLZ3gAm00CRq4e2GarcEeLAq1d7rcSxV5/84pOIX9mH7PNJTg5js6pOn9hrZWmmwRKYYVTPFqkAlTurrO0NjOT/kmDdHT0Xnaq0LRg0bKa79iDJcylT5C08PAJX5S9esPXhNwS6rxMwk1ApmdE5USs/hn3geDAebJ0aRwIwnX5+g==-----END CERTIFICATE----------BEGIN CERTIFICATE-----MIIFPDCCAySgAwIBAgIQdWSbQJ/CYk1/BERap1nTNjANBgkqhkiG9w0BAQsFADA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwHhcNMTYwMjE5MTEzODI2WhcNMjYwMjE2MTEzODI2WjA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC2boA0hxlI9cDcSrt956lPnNuQ0ek/NnuWw6dsGXZeC1RHBR5m3/0jGnKRMpW2PpmTp7eG2ngOCbZMd5tGhZHoposlCHSjRrJvhXWPRDP52WvtSgsckcJtKw1uoo3lqZGBPAHpvg3ExxcWY8Q/b7H3Rc0zY2nM/whiayJPwO4+wE3gGiQwFrvsaekFjX/bNwBnZRiSAHm6iYeL0qfwvSUwBcs0Wzh2yCKOuNqnuk+xFy3iaj5ADWGDxhm4Qf4q5UKJCriYZnWWSn0CDIHlZPc05sXERO52OCyzAG5Bm8qCGeCBtgpFQtHf72gbJFfSqJKs0VQ7U8N9ucI3NAEpvt0NDXALF+4EoyV+0vCrq4U5f7geUSKPSEZWI2lpyz+NCZ95BrTFSHLDuNsTXJLnmJjOYCymAm5luKA6DQw3HyNXIndgKV2e5BfhSWx3HP4J0DxO7kB2F1APUSijNZAe2x6x+SO85CR6dT46pEvoGypD8EiRRoDHbQ8Vw1ulVh8nXoJzCs8v3exQUt1ZG7G1Pcmp/S4xPF0Y6/HP0IIe2pxJ4uzOYaaARki3AI8pwHfD6OON6tRC0wjnPB4qYPtanVJo4Nr/UWmq8vpgLKrI2kE3ceiPkNgb7/cXepyseTDBQidvCwV/ZyixmafwDgDi02zN/FI4yS3aMtpyXtrTkKlLXwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQU1T68/Oa44A7bgn6wxN1deQVukKQwDQYJKoZIhvcNAQELBQADggIBAEGTbgPUdRcWCgLclrbIOJ9wNC/T0LhmAuMyPtXJVfojaH1XlWWGZw9CTAD0/d/W1cE0QwLi7MI0IWV6Lb4VjaogXIga7ND5uLzZ5iJb7SK8+gjK0d8hpGUKrwLzS6jUuL4vieM9DF7/VPi4EJm4EL35QfNpnb4Y17yOY1FZwZjtwlPZWGrG0plRTi70/Mgic4a3KtC1I33RUUruF3nk+Fm+VEJJzmoOi01JwDwuM1hT6lI4USNLp2vy4l1iJSdBSlwwNEthv1C/eHqC2XkH8Kr6kufW8s2Cnqu1tHJ/U+ns/m5dDcrP22i/toDKVwOdquFdB4bg42PWyKeQi85UlHVSPwlTiB7gXZi97vtIDlIfYZ6V3zy4fSUudaBXEm4IOY7IoRFB1zoqSj86KtufjOLAfqAcUFqYJGKEIfjbGistagDKh5VRTtmgWnCSp252h27UHrYMWSv9/oi6H7m9dv5ZBuUgeYnxsgZYDgic4xA80POOWAiMwYdoIQwQghdGLRDuXT8krg8/ery42xmIvqW0xpJzROAVzWgtEUFFtFfMnrFjf2b4o6Mw8A6AflbL1zeRuum/Uz+sFVVSUS1uzWrIRSTN6M2tRpu6EuRuNCJkNXxqQ5v3iBCpoKsXEBqDeymnT4WEFqv+Rq2ZHbticZ+vXbu8039fau7bdmVS9Bjj-----END CERTIFICATE-----"
  val caString = "-----BEGIN CERTIFICATE-----MIIFOjCCAyKgAwIBAgIQdWSbQJ/CYk1/BERap1nTPTANBgkqhkiG9w0BAQsFADA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwHhcNMTYxMDIxMTUxNjEyWhcNMjYxMDE5MTUxNjEyWjA2MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEVMBMGA1UEAwwMTGFicyB0ZWFtIENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtMw5w9bxjXur+T3A/sFEcDCKxvKWU1Um86puh36D3Zc55/aTpNzq8UqyEtTUbr9xuqpkriQWPZcS3CZEDW0wnEXmZ6/ukQ059T9wv6P+YGEXMqaVHn3qPGwiZ8WbClVrGfDwTl+9sfqvfR6keJq8rJkxE21ECW94ikI7Tk19s0Rz62xf+/FFRndQbTsech9Opi4TC2zMd9h9rPyRwfSmVHMKPmqm+nnAoDjBlUalxjt+n7Vs269ZBqfusn25Em+BIMwU4z13csHIuZuB/mvmqipxc2VHHrvhlCeoSgqWnNvmok4D8Ug+9sASAKYn1stdkSxCqwLLWb9PR/fUcqorvz1S8KNc07c7IIt2ip5sXGWSJKIazak4CHiJGEQ11jO4NOFsXp+tDaQgT/21Aq45zHJyf9ymT8c0ztwe9oQI1UYXHyopfyW1o9Ca5/MxQeXWcCPEFE1IMoA/e6vDWF09liDOZl94lSwZ2ev+zGGDN+WU4ZojCbjdLCv/tbuSdyiTd4jV6dCShObyWMc6l+TAfHvHFkxD29MDC165evoSXWMZtqriyt2h/9kWmqgblpz0oJ4kFPQ9RY5n34FHMSvabkSTWkjcV8m6/4YDaRWYTo7a+ObJ9a6sk0J8pfYYcpwn4QoFS+12IA95ea51TWN1bEoIFaVEqBDy5y3Qe7JSWNkCAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFAkpP4Zm5qJ0Frn14iTtthSCrXVSMA0GCSqGSIb3DQEBCwUAA4ICAQA/Pnv9Lf/Ic/EeJYjFGZ84bICIZ/cFFNPcb4lWc7zJNOg2690t748MwqDUQYvCL4f5Dh+rvssLxK/FlMGDiw4UrZGHdHS5u3bTTUrbwG1NHQKRnthdz1xtx/TlBn4NvqVokcB7AkC5XVwpTvvVvcgpRp+ffYK3gd6arOb6oOOzRYDawFgPEZdZrbREh9p1k0NpbkUcmNiNMjQhV+DqVvGI+pYTOP4686Q4PvC7OGma5X3T0MWwUJ3iMPdo4qAGJB8GV4XbyPapMd7vqFzsf18yAKmT9Q2afr1U1KCQ/zourY1uqBeor5CzJzCDqLDxgXrBauVwB844Epa24x98RQGfZuIULZGPn0Sf8sF4L2bwmBBQM29yAWO4DLEZU0yqQTUHV7oJmNN+i0iE4wNSlC/Gz/wV3LCG+tseQkgvRziN8hwoTGg6n9EvT2NdD0QpzJ2D7PQeJIytKEYzJKVRCZkn3Xcoy1SzIKiG0xDLME22rjFuvWFyw1r/OnErIM7RL4GLZ3gAm00CRq4e2GarcEeLAq1d7rcSxV5/84pOIX9mH7PNJTg5js6pOn9hrZWmmwRKYYVTPFqkAlTurrO0NjOT/kmDdHT0Xnaq0LRg0bKa79iDJcylT5C08PAJX5S9esPXhNwS6rxMwk1ApmdE5USs/hn3geDAebJ0aRwIwnX5+g==-----END CERTIFICATE----------BEGIN CERTIFICATE-----MIIFPDCCAySgAwIBAgIQdWSbQJ/CYk1/BERap1nTNjANBgkqhkiG9w0BAQsFADA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwHhcNMTYwMjE5MTEzODI2WhcNMjYwMjE2MTEzODI2WjA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC2boA0hxlI9cDcSrt956lPnNuQ0ek/NnuWw6dsGXZeC1RHBR5m3/0jGnKRMpW2PpmTp7eG2ngOCbZMd5tGhZHoposlCHSjRrJvhXWPRDP52WvtSgsckcJtKw1uoo3lqZGBPAHpvg3ExxcWY8Q/b7H3Rc0zY2nM/whiayJPwO4+wE3gGiQwFrvsaekFjX/bNwBnZRiSAHm6iYeL0qfwvSUwBcs0Wzh2yCKOuNqnuk+xFy3iaj5ADWGDxhm4Qf4q5UKJCriYZnWWSn0CDIHlZPc05sXERO52OCyzAG5Bm8qCGeCBtgpFQtHf72gbJFfSqJKs0VQ7U8N9ucI3NAEpvt0NDXALF+4EoyV+0vCrq4U5f7geUSKPSEZWI2lpyz+NCZ95BrTFSHLDuNsTXJLnmJjOYCymAm5luKA6DQw3HyNXIndgKV2e5BfhSWx3HP4J0DxO7kB2F1APUSijNZAe2x6x+SO85CR6dT46pEvoGypD8EiRRoDHbQ8Vw1ulVh8nXoJzCs8v3exQUt1ZG7G1Pcmp/S4xPF0Y6/HP0IIe2pxJ4uzOYaaARki3AI8pwHfD6OON6tRC0wjnPB4qYPtanVJo4Nr/UWmq8vpgLKrI2kE3ceiPkNgb7/cXepyseTDBQidvCwV/ZyixmafwDgDi02zN/FI4yS3aMtpyXtrTkKlLXwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQU1T68/Oa44A7bgn6wxN1deQVukKQwDQYJKoZIhvcNAQELBQADggIBAEGTbgPUdRcWCgLclrbIOJ9wNC/T0LhmAuMyPtXJVfojaH1XlWWGZw9CTAD0/d/W1cE0QwLi7MI0IWV6Lb4VjaogXIga7ND5uLzZ5iJb7SK8+gjK0d8hpGUKrwLzS6jUuL4vieM9DF7/VPi4EJm4EL35QfNpnb4Y17yOY1FZwZjtwlPZWGrG0plRTi70/Mgic4a3KtC1I33RUUruF3nk+Fm+VEJJzmoOi01JwDwuM1hT6lI4USNLp2vy4l1iJSdBSlwwNEthv1C/eHqC2XkH8Kr6kufW8s2Cnqu1tHJ/U+ns/m5dDcrP22i/toDKVwOdquFdB4bg42PWyKeQi85UlHVSPwlTiB7gXZi97vtIDlIfYZ6V3zy4fSUudaBXEm4IOY7IoRFB1zoqSj86KtufjOLAfqAcUFqYJGKEIfjbGistagDKh5VRTtmgWnCSp252h27UHrYMWSv9/oi6H7m9dv5ZBuUgeYnxsgZYDgic4xA80POOWAiMwYdoIQwQghdGLRDuXT8krg8/ery42xmIvqW0xpJzROAVzWgtEUFFtFfMnrFjf2b4o6Mw8A6AflbL1zeRuum/Uz+sFVVSUS1uzWrIRSTN6M2tRpu6EuRuNCJkNXxqQ5v3iBCpoKsXEBqDeymnT4WEFqv+Rq2ZHbticZ+vXbu8039fau7bdmVS9Bjj-----END CERTIFICATE-----"
  val caRootString = "-----BEGIN CERTIFICATE-----MIIFPDCCAySgAwIBAgIQdWSbQJ/CYk1/BERap1nTNjANBgkqhkiG9w0BAQsFADA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwHhcNMTYwMjE5MTEzODI2WhcNMjYwMjE2MTEzODI2WjA4MQswCQYDVQQGEwJFUzEQMA4GA1UECgwHU3RyYXRpbzEXMBUGA1UEAwwOU3RyYXRpbyBJbmMgQ0EwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC2boA0hxlI9cDcSrt956lPnNuQ0ek/NnuWw6dsGXZeC1RHBR5m3/0jGnKRMpW2PpmTp7eG2ngOCbZMd5tGhZHoposlCHSjRrJvhXWPRDP52WvtSgsckcJtKw1uoo3lqZGBPAHpvg3ExxcWY8Q/b7H3Rc0zY2nM/whiayJPwO4+wE3gGiQwFrvsaekFjX/bNwBnZRiSAHm6iYeL0qfwvSUwBcs0Wzh2yCKOuNqnuk+xFy3iaj5ADWGDxhm4Qf4q5UKJCriYZnWWSn0CDIHlZPc05sXERO52OCyzAG5Bm8qCGeCBtgpFQtHf72gbJFfSqJKs0VQ7U8N9ucI3NAEpvt0NDXALF+4EoyV+0vCrq4U5f7geUSKPSEZWI2lpyz+NCZ95BrTFSHLDuNsTXJLnmJjOYCymAm5luKA6DQw3HyNXIndgKV2e5BfhSWx3HP4J0DxO7kB2F1APUSijNZAe2x6x+SO85CR6dT46pEvoGypD8EiRRoDHbQ8Vw1ulVh8nXoJzCs8v3exQUt1ZG7G1Pcmp/S4xPF0Y6/HP0IIe2pxJ4uzOYaaARki3AI8pwHfD6OON6tRC0wjnPB4qYPtanVJo4Nr/UWmq8vpgLKrI2kE3ceiPkNgb7/cXepyseTDBQidvCwV/ZyixmafwDgDi02zN/FI4yS3aMtpyXtrTkKlLXwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQU1T68/Oa44A7bgn6wxN1deQVukKQwDQYJKoZIhvcNAQELBQADggIBAEGTbgPUdRcWCgLclrbIOJ9wNC/T0LhmAuMyPtXJVfojaH1XlWWGZw9CTAD0/d/W1cE0QwLi7MI0IWV6Lb4VjaogXIga7ND5uLzZ5iJb7SK8+gjK0d8hpGUKrwLzS6jUuL4vieM9DF7/VPi4EJm4EL35QfNpnb4Y17yOY1FZwZjtwlPZWGrG0plRTi70/Mgic4a3KtC1I33RUUruF3nk+Fm+VEJJzmoOi01JwDwuM1hT6lI4USNLp2vy4l1iJSdBSlwwNEthv1C/eHqC2XkH8Kr6kufW8s2Cnqu1tHJ/U+ns/m5dDcrP22i/toDKVwOdquFdB4bg42PWyKeQi85UlHVSPwlTiB7gXZi97vtIDlIfYZ6V3zy4fSUudaBXEm4IOY7IoRFB1zoqSj86KtufjOLAfqAcUFqYJGKEIfjbGistagDKh5VRTtmgWnCSp252h27UHrYMWSv9/oi6H7m9dv5ZBuUgeYnxsgZYDgic4xA80POOWAiMwYdoIQwQghdGLRDuXT8krg8/ery42xmIvqW0xpJzROAVzWgtEUFFtFfMnrFjf2b4o6Mw8A6AflbL1zeRuum/Uz+sFVVSUS1uzWrIRSTN6M2tRpu6EuRuNCJkNXxqQ5v3iBCpoKsXEBqDeymnT4WEFqv+Rq2ZHbticZ+vXbu8039fau7bdmVS9Bjj-----END CERTIFICATE-----"
  // scalastyle:on

  def calculateMD5(path: String): String = {
    val file = Files readAllBytes (Paths get path)
    val checksum = MessageDigest.getInstance("MD5") digest file
    checksum.map("%02X" format _).mkString
  }

  test("generate pkcs8 from valid key" ) {
    SSLConfig.pemToDer(Source.fromURL(getClass.getResource("/cert.key")).mkString)
    assert (
      calculateMD5(getClass.getResource("/key.pkcs8").getFile)
        .equals(calculateMD5("/tmp/key.pkcs8")))
  }

  test("generate cert.crt from valid data") {
    SSLConfig.generatePemFile( pemString, "/cert.crt")
    assert (
      calculateMD5(getClass.getResource("/cert.crt").getFile)
        .equals(calculateMD5("/tmp/cert.crt")))
  }

  test("generate ca-two-levels.crt from intermediate chain valid data") {
    SSLConfig.generatePemFile( caString, "/ca-two-levels.crt")
    assert (
      calculateMD5(getClass.getResource("/ca-two-levels.crt").getFile)
        .equals(calculateMD5("/tmp/ca-two-levels.crt")))
  }

  test("generate ca-two-levels.crt from valid root ca") {
    SSLConfig.generatePemFile( caRootString, "/ca-one-level.crt")
    assert (
      calculateMD5(getClass.getResource("/ca-one-level.crt").getFile)
        .equals(calculateMD5("/tmp/ca-one-level.crt")))
  }

}
