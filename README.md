# taxserver [![Build Status](https://travis-ci.com/taxcalcs/taxserver.svg?branch=master)](https://travis-ci.com/taxcalcs/taxserver) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d4aebdfc5a941399fe0704ebcf179e8)](https://app.codacy.com/gh/taxcalcs/taxserver)

Example server for the tax calculation.

## Example

Base URL: http://localhost:8080/< year >/<month or 0>

Paremeter: add all input parameter as query parameter

Example URL: http://localhost:8080/2021/0?LZZ=1&RE4=6500000&STKL=1

Result

```xml
<lohnsteuer jahr="2021">
  <information>Created by info.kuechler.bmf.test.server</information>
    <eingaben>
    <eingabe name="LZZ" value="1" status="ok"/>
    <eingabe name="RE4" value="6500000" status="ok"/>
    <eingabe name="STKL" value="1" status="ok"/>
  </eingaben>
  <ausgaben>
    <ausgabe name="BK" value="0" type="STANDARD"/>
    <ausgabe name="BKS" value="0" type="STANDARD"/>
    <ausgabe name="BKV" value="0" type="STANDARD"/>
    <ausgabe name="LSTLZZ" value="1355000" type="STANDARD"/>
    <ausgabe name="SOLZLZZ" value="0" type="STANDARD"/>
    <ausgabe name="SOLZS" value="0" type="STANDARD"/>
    <ausgabe name="SOLZV" value="0" type="STANDARD"/>
    <ausgabe name="STS" value="0" type="STANDARD"/>
    <ausgabe name="STV" value="0" type="STANDARD"/>
    <ausgabe name="VFRB" value="100000" type="DBA"/>
    <ausgabe name="VFRBS1" value="0" type="DBA"/>
    <ausgabe name="VFRBS2" value="0" type="DBA"/>
    <ausgabe name="VKVLZZ" value="0" type="STANDARD"/>
    <ausgabe name="VKVSONST" value="0" type="STANDARD"/>
    <ausgabe name="WVFRB" value="4419300" type="DBA"/>
    <ausgabe name="WVFRBM" value="0" type="DBA"/>
    <ausgabe name="WVFRBO" value="0" type="DBA"/>
  </ausgaben>
</lohnsteuer>
```
