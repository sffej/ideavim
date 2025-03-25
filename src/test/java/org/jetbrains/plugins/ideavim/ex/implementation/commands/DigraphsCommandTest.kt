/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.DigraphCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class DigraphsCommandTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test digraphs is parsed correctly`() {
    val exCommand = injector.vimscriptParser.parseCommand("digraphs")
    assertTrue(exCommand is DigraphCommand)
  }

  @Test
  fun `test add custom digraph`() {
    enterCommand("digraph (0 9450")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('(', '0').toChar())
  }

  @Test
  fun `test add custom 32-bit digraph`() {
    enterCommand("digraph cr 128308")
    assertEquals("🔴", String(Character.toChars(injector.digraphGroup.getCharacterForDigraph('c', 'r'))))
  }

  @Test
  fun `test add custom digraph matches reversed characters`() {
    enterCommand("digraph (0 9450")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('0', '(').toChar())
  }

  @Test
  fun `test add multiple custom digraphs`() {
    enterCommand("digraph (0 9450 (1 9312")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('(', '0').toChar())
    assertEquals('①', injector.digraphGroup.getCharacterForDigraph('(', '1').toChar())
  }

  @Test
  fun `test add custom digraph with single character reports error`() {
    enterCommand("digraph a")
    assertPluginError(true)
    assertPluginErrorMessageContains("E1214: Digraph must be just two characters: a")
  }

  @Test
  fun `test add custom digraph with too many characters reports error about missing codepoint`() {
    enterCommand("digraph aaaaa")
    assertPluginError(true)
    assertPluginErrorMessageContains("E39: Number expected")
  }

  @Test
  fun `test add custom digraph without codepoint reports error`() {
    enterCommand("digraph aa")
    assertPluginError(true)
    assertPluginErrorMessageContains("E39: Number expected")
  }

  @Test
  fun `test add custom digraph with invalid codepoint reports error`() {
    enterCommand("digraph aa nvnvn")
    assertPluginError(true)
    assertPluginErrorMessageContains("E39: Number expected")
  }

  @Test
  fun `test add custom digraph with more than two characters add custom digraph with initial two characters`() {
    enterCommand("digraph aaaa 9450")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('a', 'a').toChar())
  }

  @Test
  fun `test add custom digraphs until error`() {
    enterCommand("digraph (0 9450 (1 9312 (2")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('(', '0').toChar())
    assertEquals('①', injector.digraphGroup.getCharacterForDigraph('(', '1').toChar())
    assertPluginError(true)
    assertPluginErrorMessageContains("E39: Number expected")
  }

  @Test
  fun `test custom digraph overwrites existing custom digraph`() {
    enterCommand("digraph (0 9450")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('(', '0').toChar())
    enterCommand("digraph (0 10003")
    assertEquals('✓', injector.digraphGroup.getCharacterForDigraph('(', '0').toChar())
  }

  @Test
  fun `test custom digraph overwrites existing default digraph`() {
    enterCommand("digraph OK 9450")
    assertEquals('⓪', injector.digraphGroup.getCharacterForDigraph('O', 'K').toChar())
  }

  @Test
  fun `test digraph output`() {
    // Note that the following text has some control characters in, notably Unicode's RLI and PDI characters to isolate
    // RTL characters and then reset the isolation.
    // Other control characters are either encoded or left as-is, matching Vim behaviour.
    // This output is a very close match to Vim's output as captured with `redir @">|silent digraphs|redir END|enew|put`
    // Differences:
    // * Unexpected changes in column width in Vim's output (goes from 13 to 12?)
    // * `LF` is represented by `^@` in Vim, although it's output as a LF. Weird Vim NL/null handling
    // * Additional Unicode control characters to ensure that RTL doesn't affect the rest of the line. We add the RLI
    //   RIGHT_TO_LEFT_ISOLATE code to start an isolated run of RTL chars, then POP_DIRECTIONAL_ISOLATE to restore
    assertCommandOutput(
      "digraphs",
      """
        |NU ^@  10    SH ^A   1    SX ^B   2    EX ^C   3    ET ^D   4    EQ ^E   5
        |AK ^F   6    BL ^G   7    BS ^H   8    HT ^I   9    LF ^J  10    VT ^K  11
        |FF ^L  12    CR ^M  13    SO ^N  14    SI ^O  15    DL ^P  16    D1 ^Q  17
        |D2 ^R  18    D3 ^S  19    D4 ^T  20    NK ^U  21    SY ^V  22    EB ^W  23
        |CN ^X  24    EM ^Y  25    SB ^Z  26    EC ^[  27    FS ^\  28    GS ^]  29
        |RS ^^  30    US ^_  31    SP     32    Nb #   35    DO $   36    At @   64
        |<( [   91    // \   92    )> ]   93    '> ^   94    '! `   96    (! {  123
        |!! |  124    !) }  125    '? ~  126    DT ^? 127    PA <80> 128  HO <81> 129
        |BH <82> 130  NH <83> 131  IN <84> 132  NL <85> 133  SA <86> 134  ES <87> 135
        |HS <88> 136  HJ <89> 137  VS <8a> 138  PD <8b> 139  PU <8c> 140  RI <8d> 141
        |S2 <8e> 142  S3 <8f> 143  DC <90> 144  P1 <91> 145  P2 <92> 146  TS <93> 147
        |CC <94> 148  MW <95> 149  SG <96> 150  EG <97> 151  SS <98> 152  GC <99> 153
        |SC <9a> 154  CI <9b> 155  ST <9c> 156  OC <9d> 157  PM <9e> 158  AC <9f> 159
        |NS    160    !I ¡  161    ~! ¡  161    Ct ¢  162    c| ¢  162    Pd £  163
        |$$ £  163    Cu ¤  164    ox ¤  164    Ye ¥  165    Y- ¥  165    BB ¦  166
        ||| ¦  166    SE §  167    ': ¨  168    Co ©  169    cO ©  169    -a ª  170
        |<< «  171    NO ¬  172    -, ¬  172    -- <ad> 173  Rg ®  174    'm ¯  175
        |-= ¯  175    DG °  176    ~o °  176    +- ±  177    2S ²  178    22 ²  178
        |3S ³  179    33 ³  179    '' ´  180    My µ  181    PI ¶  182    pp ¶  182
        |.M ·  183    ~. ·  183    ', ¸  184    1S ¹  185    11 ¹  185    -o º  186
        |>> »  187    14 ¼  188    12 ½  189    34 ¾  190    ?I ¿  191    ~? ¿  191
        |A! À  192    A` À  192    A' Á  193    A> Â  194    A^ Â  194    A? Ã  195
        |A~ Ã  195    A: Ä  196    A" Ä  196    AA Å  197    A@ Å  197    AE Æ  198
        |C, Ç  199    E! È  200    E` È  200    E' É  201    E> Ê  202    E^ Ê  202
        |E: Ë  203    E" Ë  203    I! Ì  204    I` Ì  204    I' Í  205    I> Î  206
        |I^ Î  206    I: Ï  207    I" Ï  207    D- Ð  208    N? Ñ  209    N~ Ñ  209
        |O! Ò  210    O` Ò  210    O' Ó  211    O> Ô  212    O^ Ô  212    O? Õ  213
        |O~ Õ  213    O: Ö  214    *X ×  215    /\ ×  215    O/ Ø  216    U! Ù  217
        |U` Ù  217    U' Ú  218    U> Û  219    U^ Û  219    U: Ü  220    Y' Ý  221
        |TH Þ  222    Ip Þ  222    ss ß  223    a! à  224    a` à  224    a' á  225
        |a> â  226    a^ â  226    a? ã  227    a~ ã  227    a: ä  228    a" ä  228
        |aa å  229    a@ å  229    ae æ  230    c, ç  231    e! è  232    e` è  232
        |e' é  233    e> ê  234    e^ ê  234    e: ë  235    e" ë  235    i! ì  236
        |i` ì  236    i' í  237    i> î  238    i^ î  238    i: ï  239    d- ð  240
        |n? ñ  241    n~ ñ  241    o! ò  242    o` ò  242    o' ó  243    o> ô  244
        |o^ ô  244    o? õ  245    o~ õ  245    o: ö  246    -: ÷  247    o/ ø  248
        |u! ù  249    u` ù  249    u' ú  250    u> û  251    u^ û  251    u: ü  252
        |y' ý  253    th þ  254    y: ÿ  255    y" ÿ  255    A- Ā  256    a- ā  257
        |A( Ă  258    a( ă  259    A; Ą  260    a; ą  261    C' Ć  262    c' ć  263
        |C> Ĉ  264    c> ĉ  265    C. Ċ  266    c. ċ  267    C< Č  268    c< č  269
        |D< Ď  270    d< ď  271    D/ Đ  272    d/ đ  273    E- Ē  274    e- ē  275
        |E( Ĕ  276    e( ĕ  277    E. Ė  278    e. ė  279    E; Ę  280    e; ę  281
        |E< Ě  282    e< ě  283    G> Ĝ  284    g> ĝ  285    G( Ğ  286    g( ğ  287
        |G. Ġ  288    g. ġ  289    G, Ģ  290    g, ģ  291    H> Ĥ  292    h> ĥ  293
        |H/ Ħ  294    h/ ħ  295    I? Ĩ  296    i? ĩ  297    I- Ī  298    i- ī  299
        |I( Ĭ  300    i( ĭ  301    I; Į  302    i; į  303    I. İ  304    i. ı  305
        |IJ Ĳ  306    ij ĳ  307    J> Ĵ  308    j> ĵ  309    K, Ķ  310    k, ķ  311
        |kk ĸ  312    L' Ĺ  313    l' ĺ  314    L, Ļ  315    l, ļ  316    L< Ľ  317
        |l< ľ  318    L. Ŀ  319    l. ŀ  320    L/ Ł  321    l/ ł  322    N' Ń  323
        |n' ń  324    N, Ņ  325    n, ņ  326    N< Ň  327    n< ň  328    'n ŉ  329
        |NG Ŋ  330    ng ŋ  331    O- Ō  332    o- ō  333    O( Ŏ  334    o( ŏ  335
        |O" Ő  336    o" ő  337    OE Œ  338    oe œ  339    R' Ŕ  340    r' ŕ  341
        |R, Ŗ  342    r, ŗ  343    R< Ř  344    r< ř  345    S' Ś  346    s' ś  347
        |S> Ŝ  348    s> ŝ  349    S, Ş  350    s, ş  351    S< Š  352    s< š  353
        |T, Ţ  354    t, ţ  355    T< Ť  356    t< ť  357    T/ Ŧ  358    t/ ŧ  359
        |U? Ũ  360    u? ũ  361    U- Ū  362    u- ū  363    U( Ŭ  364    u( ŭ  365
        |U0 Ů  366    u0 ů  367    U" Ű  368    u" ű  369    U; Ų  370    u; ų  371
        |W> Ŵ  372    w> ŵ  373    Y> Ŷ  374    y> ŷ  375    Y: Ÿ  376    Z' Ź  377
        |z' ź  378    Z. Ż  379    z. ż  380    Z< Ž  381    z< ž  382    O9 Ơ  416
        |o9 ơ  417    OI Ƣ  418    oi ƣ  419    yr Ʀ  422    U9 Ư  431    u9 ư  432
        |Z/ Ƶ  437    z/ ƶ  438    ED Ʒ  439    A< Ǎ  461    a< ǎ  462    I< Ǐ  463
        |i< ǐ  464    O< Ǒ  465    o< ǒ  466    U< Ǔ  467    u< ǔ  468    A1 Ǟ  478
        |a1 ǟ  479    A7 Ǡ  480    a7 ǡ  481    A3 Ǣ  482    a3 ǣ  483    G/ Ǥ  484
        |g/ ǥ  485    G< Ǧ  486    g< ǧ  487    K< Ǩ  488    k< ǩ  489    O; Ǫ  490
        |o; ǫ  491    O1 Ǭ  492    o1 ǭ  493    EZ Ǯ  494    ez ǯ  495    j< ǰ  496
        |G' Ǵ  500    g' ǵ  501    ;S ʿ  703    '< ˇ  711    '( ˘  728    '. ˙  729
        |'0 ˚  730    '; ˛  731    '" ˝  733    A% Ά  902    E% Έ  904    Y% Ή  905
        |I% Ί  906    O% Ό  908    U% Ύ  910    W% Ώ  911    i3 ΐ  912    A* Α  913
        |B* Β  914    G* Γ  915    D* Δ  916    E* Ε  917    Z* Ζ  918    Y* Η  919
        |H* Θ  920    I* Ι  921    K* Κ  922    L* Λ  923    M* Μ  924    N* Ν  925
        |C* Ξ  926    O* Ο  927    P* Π  928    R* Ρ  929    S* Σ  931    T* Τ  932
        |U* Υ  933    F* Φ  934    X* Χ  935    Q* Ψ  936    W* Ω  937    J* Ϊ  938
        |V* Ϋ  939    a% ά  940    e% έ  941    y% ή  942    i% ί  943    u3 ΰ  944
        |a* α  945    b* β  946    g* γ  947    d* δ  948    e* ε  949    z* ζ  950
        |y* η  951    h* θ  952    i* ι  953    k* κ  954    l* λ  955    m* μ  956
        |n* ν  957    c* ξ  958    o* ο  959    p* π  960    r* ρ  961    *s ς  962
        |s* σ  963    t* τ  964    u* υ  965    f* φ  966    x* χ  967    q* ψ  968
        |w* ω  969    j* ϊ  970    v* ϋ  971    o% ό  972    u% ύ  973    w% ώ  974
        |'G Ϙ  984    ,G ϙ  985    T3 Ϛ  986    t3 ϛ  987    M3 Ϝ  988    m3 ϝ  989
        |K3 Ϟ  990    k3 ϟ  991    P3 Ϡ  992    p3 ϡ  993    '% ϴ  1012   j3 ϵ  1013
        |IO Ё  1025   D% Ђ  1026   G% Ѓ  1027   IE Є  1028   DS Ѕ  1029   II І  1030
        |YI Ї  1031   J% Ј  1032   LJ Љ  1033   NJ Њ  1034   Ts Ћ  1035   KJ Ќ  1036
        |V% Ў  1038   DZ Џ  1039   A= А  1040   B= Б  1041   V= В  1042   G= Г  1043
        |D= Д  1044   E= Е  1045   Z% Ж  1046   Z= З  1047   I= И  1048   J= Й  1049
        |K= К  1050   L= Л  1051   M= М  1052   N= Н  1053   O= О  1054   P= П  1055
        |R= Р  1056   S= С  1057   T= Т  1058   U= У  1059   F= Ф  1060   H= Х  1061
        |C= Ц  1062   C% Ч  1063   S% Ш  1064   Sc Щ  1065   =" Ъ  1066   Y= Ы  1067
        |%" Ь  1068   JE Э  1069   JU Ю  1070   JA Я  1071   a= а  1072   b= б  1073
        |v= в  1074   g= г  1075   d= д  1076   e= е  1077   z% ж  1078   z= з  1079
        |i= и  1080   j= й  1081   k= к  1082   l= л  1083   m= м  1084   n= н  1085
        |o= о  1086   p= п  1087   r= р  1088   s= с  1089   t= т  1090   u= у  1091
        |f= ф  1092   h= х  1093   c= ц  1094   c% ч  1095   s% ш  1096   sc щ  1097
        |=' ъ  1098   y= ы  1099   %' ь  1100   je э  1101   ju ю  1102   ja я  1103
        |io ё  1105   d% ђ  1106   g% ѓ  1107   ie є  1108   ds ѕ  1109   ii і  1110
        |yi ї  1111   j% ј  1112   lj љ  1113   nj њ  1114   ts ћ  1115   kj ќ  1116
        |v% ў  1118   dz џ  1119   Y3 Ѣ  1122   y3 ѣ  1123   O3 Ѫ  1130   o3 ѫ  1131
        |F3 Ѳ  1138   f3 ѳ  1139   V3 Ѵ  1140   v3 ѵ  1141   C3 Ҁ  1152   c3 ҁ  1153
        |G3 Ґ  1168   g3 ґ  1169   A+ ⁧א⁩  1488   B+ ⁧ב⁩  1489   G+ ⁧ג⁩  1490   D+ ⁧ד⁩  1491
        |H+ ⁧ה⁩  1492   W+ ⁧ו⁩  1493   Z+ ⁧ז⁩  1494   X+ ⁧ח⁩  1495   Tj ⁧ט⁩  1496   J+ ⁧י⁩  1497
        |K% ⁧ך⁩  1498   K+ ⁧כ⁩  1499   L+ ⁧ל⁩  1500   M% ⁧ם⁩  1501   M+ ⁧מ⁩  1502   N% ⁧ן⁩  1503
        |N+ ⁧נ⁩  1504   S+ ⁧ס⁩  1505   E+ ⁧ע⁩  1506   P% ⁧ף⁩  1507   P+ ⁧פ⁩  1508   Zj ⁧ץ⁩  1509
        |ZJ ⁧צ⁩  1510   Q+ ⁧ק⁩  1511   R+ ⁧ר⁩  1512   Sh ⁧ש⁩  1513   T+ ⁧ת⁩  1514   ,+ ،  1548
        |;+ ⁧؛⁩  1563   ?+ ⁧؟⁩  1567   H' ⁧ء⁩  1569   aM ⁧آ⁩  1570   aH ⁧أ⁩  1571   wH ⁧ؤ⁩  1572
        |ah ⁧إ⁩  1573   yH ⁧ئ⁩  1574   a+ ⁧ا⁩  1575   b+ ⁧ب⁩  1576   tm ⁧ة⁩  1577   t+ ⁧ت⁩  1578
        |tk ⁧ث⁩  1579   g+ ⁧ج⁩  1580   hk ⁧ح⁩  1581   x+ ⁧خ⁩  1582   d+ ⁧د⁩  1583   dk ⁧ذ⁩  1584
        |r+ ⁧ر⁩  1585   z+ ⁧ز⁩  1586   s+ ⁧س⁩  1587   sn ⁧ش⁩  1588   c+ ⁧ص⁩  1589   dd ⁧ض⁩  1590
        |tj ⁧ط⁩  1591   zH ⁧ظ⁩  1592   e+ ⁧ع⁩  1593   i+ ⁧غ⁩  1594   ++ ⁧ـ⁩  1600   f+ ⁧ف⁩  1601
        |q+ ⁧ق⁩  1602   k+ ⁧ك⁩  1603   l+ ⁧ل⁩  1604   m+ ⁧م⁩  1605   n+ ⁧ن⁩  1606   h+ ⁧ه⁩  1607
        |w+ ⁧و⁩  1608   j+ ⁧ى⁩  1609   y+ ⁧ي⁩  1610   :+  ً  1611   "+  ٌ  1612   =+  ٍ  1613
        |/+  َ  1614   '+  ُ  1615   1+  ِ  1616   3+  ّ  1617   0+  ْ  1618   aS  ٰ  1648
        |p+ ⁧پ⁩  1662   v+ ⁧ڤ⁩  1700   gf ⁧گ⁩  1711   0a ۰  1776   1a ۱  1777   2a ۲  1778
        |3a ۳  1779   4a ۴  1780   5a ۵  1781   6a ۶  1782   7a ۷  1783   8a ۸  1784
        |9a ۹  1785   B. Ḃ  7682   b. ḃ  7683   B_ Ḇ  7686   b_ ḇ  7687   D. Ḋ  7690
        |d. ḋ  7691   D_ Ḏ  7694   d_ ḏ  7695   D, Ḑ  7696   d, ḑ  7697   F. Ḟ  7710
        |f. ḟ  7711   G- Ḡ  7712   g- ḡ  7713   H. Ḣ  7714   h. ḣ  7715   H: Ḧ  7718
        |h: ḧ  7719   H, Ḩ  7720   h, ḩ  7721   K' Ḱ  7728   k' ḱ  7729   K_ Ḵ  7732
        |k_ ḵ  7733   L_ Ḻ  7738   l_ ḻ  7739   M' Ḿ  7742   m' ḿ  7743   M. Ṁ  7744
        |m. ṁ  7745   N. Ṅ  7748   n. ṅ  7749   N_ Ṉ  7752   n_ ṉ  7753   P' Ṕ  7764
        |p' ṕ  7765   P. Ṗ  7766   p. ṗ  7767   R. Ṙ  7768   r. ṙ  7769   R_ Ṟ  7774
        |r_ ṟ  7775   S. Ṡ  7776   s. ṡ  7777   T. Ṫ  7786   t. ṫ  7787   T_ Ṯ  7790
        |t_ ṯ  7791   V? Ṽ  7804   v? ṽ  7805   W! Ẁ  7808   W` Ẁ  7808   w! ẁ  7809
        |w` ẁ  7809   W' Ẃ  7810   w' ẃ  7811   W: Ẅ  7812   w: ẅ  7813   W. Ẇ  7814
        |w. ẇ  7815   X. Ẋ  7818   x. ẋ  7819   X: Ẍ  7820   x: ẍ  7821   Y. Ẏ  7822
        |y. ẏ  7823   Z> Ẑ  7824   z> ẑ  7825   Z_ Ẕ  7828   z_ ẕ  7829   h_ ẖ  7830
        |t: ẗ  7831   w0 ẘ  7832   y0 ẙ  7833   A2 Ả  7842   a2 ả  7843   E2 Ẻ  7866
        |e2 ẻ  7867   E? Ẽ  7868   e? ẽ  7869   I2 Ỉ  7880   i2 ỉ  7881   O2 Ỏ  7886
        |o2 ỏ  7887   U2 Ủ  7910   u2 ủ  7911   Y! Ỳ  7922   Y` Ỳ  7922   y! ỳ  7923
        |y` ỳ  7923   Y2 Ỷ  7926   y2 ỷ  7927   Y? Ỹ  7928   y? ỹ  7929   ;' ἀ  7936
        |,' ἁ  7937   ;! ἂ  7938   ,! ἃ  7939   ?; ἄ  7940   ?, ἅ  7941   !: ἆ  7942
        |?: ἇ  7943   1N    8194   1M    8195   3M    8196   4M    8197   6M    8198
        |1T    8201   1H    8202   -1 ‐  8208   -N –  8211   -M —  8212   -3 ―  8213
        |!2 ‖  8214   =2 ‗  8215   '6 ‘  8216   '9 ’  8217   .9 ‚  8218   9' ‛  8219
        |"6 “  8220   "9 ”  8221   :9 „  8222   9" ‟  8223   /- †  8224   /= ‡  8225
        |oo •  8226   .. ‥  8229   ,. …  8230   %0 ‰  8240   1' ′  8242   2' ″  8243
        |3' ‴  8244   4' ⁗  8279   1" ‵  8245   2" ‶  8246   3" ‷  8247   Ca ‸  8248
        |<1 ‹  8249   >1 ›  8250   :X ※  8251   '- ‾  8254   /f ⁄  8260   0S ⁰  8304
        |4S ⁴  8308   5S ⁵  8309   6S ⁶  8310   7S ⁷  8311   8S ⁸  8312   9S ⁹  8313
        |+S ⁺  8314   -S ⁻  8315   =S ⁼  8316   (S ⁽  8317   )S ⁾  8318   nS ⁿ  8319
        |0s ₀  8320   1s ₁  8321   2s ₂  8322   3s ₃  8323   4s ₄  8324   5s ₅  8325
        |6s ₆  8326   7s ₇  8327   8s ₈  8328   9s ₉  8329   +s ₊  8330   -s ₋  8331
        |=s ₌  8332   (s ₍  8333   )s ₎  8334   Li ₤  8356   Pt ₧  8359   W= ₩  8361
        |=e €  8364   Eu €  8364   =R ₽  8381   =P ₽  8381   oC ℃  8451   co ℅  8453
        |oF ℉  8457   N0 №  8470   PO ℗  8471   Rx ℞  8478   SM ℠  8480   TM ™  8482
        |Om Ω  8486   AO Å  8491   13 ⅓  8531   23 ⅔  8532   15 ⅕  8533   25 ⅖  8534
        |35 ⅗  8535   45 ⅘  8536   16 ⅙  8537   56 ⅚  8538   18 ⅛  8539   38 ⅜  8540
        |58 ⅝  8541   78 ⅞  8542   1R Ⅰ  8544   2R Ⅱ  8545   3R Ⅲ  8546   4R Ⅳ  8547
        |5R Ⅴ  8548   6R Ⅵ  8549   7R Ⅶ  8550   8R Ⅷ  8551   9R Ⅸ  8552   aR Ⅹ  8553
        |bR Ⅺ  8554   cR Ⅻ  8555   1r ⅰ  8560   2r ⅱ  8561   3r ⅲ  8562   4r ⅳ  8563
        |5r ⅴ  8564   6r ⅵ  8565   7r ⅶ  8566   8r ⅷ  8567   9r ⅸ  8568   ar ⅹ  8569
        |br ⅺ  8570   cr ⅻ  8571   <- ←  8592   -! ↑  8593   -> →  8594   -v ↓  8595
        |<> ↔  8596   UD ↕  8597   <= ⇐  8656   => ⇒  8658   == ⇔  8660   FA ∀  8704
        |dP ∂  8706   TE ∃  8707   /0 ∅  8709   DE ∆  8710   NB ∇  8711   (- ∈  8712
        |-) ∋  8715   *P ∏  8719   +Z ∑  8721   -2 −  8722   -+ ∓  8723   *- ∗  8727
        |Ob ∘  8728   Sb ∙  8729   RT √  8730   0( ∝  8733   00 ∞  8734   -L ∟  8735
        |-V ∠  8736   PP ∥  8741   AN ∧  8743   OR ∨  8744   (U ∩  8745   )U ∪  8746
        |In ∫  8747   DI ∬  8748   Io ∮  8750   .: ∴  8756   :. ∵  8757   :R ∶  8758
        |:: ∷  8759   ?1 ∼  8764   CG ∾  8766   ?- ≃  8771   ?= ≅  8773   ?2 ≈  8776
        |=? ≌  8780   HI ≓  8787   != ≠  8800   =3 ≡  8801   =< ≤  8804   >= ≥  8805
        |<* ≪  8810   *> ≫  8811   !< ≮  8814   !> ≯  8815   (C ⊂  8834   )C ⊃  8835
        |(_ ⊆  8838   )_ ⊇  8839   0. ⊙  8857   02 ⊚  8858   -T ⊥  8869   .P ⋅  8901
        |:3 ⋮  8942   .3 ⋯  8943   Eh ⌂  8962   <7 ⌈  8968   >7 ⌉  8969   7< ⌊  8970
        |7> ⌋  8971   NI ⌐  8976   (A ⌒  8978   TR ⌕  8981   Iu ⌠  8992   Il ⌡  8993
        |</ 〈  9001   /> 〉  9002   Vs ␣  9251   1h ⑀  9280   3h ⑁  9281   2h ⑂  9282
        |4h ⑃  9283   1j ⑆  9286   2j ⑇  9287   3j ⑈  9288   4j ⑉  9289   1. ⒈  9352
        |2. ⒉  9353   3. ⒊  9354   4. ⒋  9355   5. ⒌  9356   6. ⒍  9357   7. ⒎  9358
        |8. ⒏  9359   9. ⒐  9360   hh ─  9472   HH ━  9473   vv │  9474   VV ┃  9475
        |3- ┄  9476   3_ ┅  9477   3! ┆  9478   3/ ┇  9479   4- ┈  9480   4_ ┉  9481
        |4! ┊  9482   4/ ┋  9483   dr ┌  9484   dR ┍  9485   Dr ┎  9486   DR ┏  9487
        |dl ┐  9488   dL ┑  9489   Dl ┒  9490   LD ┓  9491   ur └  9492   uR ┕  9493
        |Ur ┖  9494   UR ┗  9495   ul ┘  9496   uL ┙  9497   Ul ┚  9498   UL ┛  9499
        |vr ├  9500   vR ┝  9501   Vr ┠  9504   VR ┣  9507   vl ┤  9508   vL ┥  9509
        |Vl ┨  9512   VL ┫  9515   dh ┬  9516   dH ┯  9519   Dh ┰  9520   DH ┳  9523
        |uh ┴  9524   uH ┷  9527   Uh ┸  9528   UH ┻  9531   vh ┼  9532   vH ┿  9535
        |Vh ╂  9538   VH ╋  9547   FD ╱  9585   BD ╲  9586   TB ▀  9600   LB ▄  9604
        |FB █  9608   lB ▌  9612   RB ▐  9616   .S ░  9617   :S ▒  9618   ?S ▓  9619
        |fS ■  9632   OS □  9633   RO ▢  9634   Rr ▣  9635   RF ▤  9636   RY ▥  9637
        |RH ▦  9638   RZ ▧  9639   RK ▨  9640   RX ▩  9641   sB ▪  9642   SR ▬  9644
        |Or ▭  9645   UT ▲  9650   uT △  9651   PR ▶  9654   Tr ▷  9655   Dt ▼  9660
        |dT ▽  9661   PL ◀  9664   Tl ◁  9665   Db ◆  9670   Dw ◇  9671   LZ ◊  9674
        |0m ○  9675   0o ◎  9678   0M ●  9679   0L ◐  9680   0R ◑  9681   Sn ◘  9688
        |Ic ◙  9689   Fd ◢  9698   Bd ◣  9699   *2 ★  9733   *1 ☆  9734   <H ☜  9756
        |>H ☞  9758   0u ☺  9786   0U ☻  9787   SU ☼  9788   Fm ♀  9792   Ml ♂  9794
        |cS ♠  9824   cH ♡  9825   cD ♢  9826   cC ♣  9827   Md ♩  9833   M8 ♪  9834
        |M2 ♫  9835   Mb ♭  9837   Mx ♮  9838   MX ♯  9839   OK ✓  10003  XX ✗  10007
        |-X ✠  10016  IS 　  12288  ,_ 、  12289  ._ 。  12290  +" 〃  12291  +_ 〄  12292
        |*_ 々  12293  ;_ 〆  12294  0_ 〇  12295  <+ 《  12298  >+ 》  12299  <' 「  12300
        |>' 」  12301  <" 『  12302  >" 』  12303  (" 【  12304  )" 】  12305  =T 〒  12306
        |=_ 〓  12307  (' 〔  12308  )' 〕  12309  (I 〖  12310  )I 〗  12311  -? 〜  12316
        |A5 ぁ  12353  a5 あ  12354  I5 ぃ  12355  i5 い  12356  U5 ぅ  12357  u5 う  12358
        |E5 ぇ  12359  e5 え  12360  O5 ぉ  12361  o5 お  12362  ka か  12363  ga が  12364
        |ki き  12365  gi ぎ  12366  ku く  12367  gu ぐ  12368  ke け  12369  ge げ  12370
        |ko こ  12371  go ご  12372  sa さ  12373  za ざ  12374  si し  12375  zi じ  12376
        |su す  12377  zu ず  12378  se せ  12379  ze ぜ  12380  so そ  12381  zo ぞ  12382
        |ta た  12383  da だ  12384  ti ち  12385  di ぢ  12386  tU っ  12387  tu つ  12388
        |du づ  12389  te て  12390  de で  12391  to と  12392  do ど  12393  na な  12394
        |ni に  12395  nu ぬ  12396  ne ね  12397  no の  12398  ha は  12399  ba ば  12400
        |pa ぱ  12401  hi ひ  12402  bi び  12403  pi ぴ  12404  hu ふ  12405  bu ぶ  12406
        |pu ぷ  12407  he へ  12408  be べ  12409  pe ぺ  12410  ho ほ  12411  bo ぼ  12412
        |po ぽ  12413  ma ま  12414  mi み  12415  mu む  12416  me め  12417  mo も  12418
        |yA ゃ  12419  ya や  12420  yU ゅ  12421  yu ゆ  12422  yO ょ  12423  yo よ  12424
        |ra ら  12425  ri り  12426  ru る  12427  re れ  12428  ro ろ  12429  wA ゎ  12430
        |wa わ  12431  wi ゐ  12432  we ゑ  12433  wo を  12434  n5 ん  12435  vu ゔ  12436
        |"5 ゛  12443  05 ゜  12444  *5 ゝ  12445  +5 ゞ  12446  a6 ァ  12449  A6 ア  12450
        |i6 ィ  12451  I6 イ  12452  u6 ゥ  12453  U6 ウ  12454  e6 ェ  12455  E6 エ  12456
        |o6 ォ  12457  O6 オ  12458  Ka カ  12459  Ga ガ  12460  Ki キ  12461  Gi ギ  12462
        |Ku ク  12463  Gu グ  12464  Ke ケ  12465  Ge ゲ  12466  Ko コ  12467  Go ゴ  12468
        |Sa サ  12469  Za ザ  12470  Si シ  12471  Zi ジ  12472  Su ス  12473  Zu ズ  12474
        |Se セ  12475  Ze ゼ  12476  So ソ  12477  Zo ゾ  12478  Ta タ  12479  Da ダ  12480
        |Ti チ  12481  Di ヂ  12482  TU ッ  12483  Tu ツ  12484  Du ヅ  12485  Te テ  12486
        |De デ  12487  To ト  12488  Do ド  12489  Na ナ  12490  Ni ニ  12491  Nu ヌ  12492
        |Ne ネ  12493  No ノ  12494  Ha ハ  12495  Ba バ  12496  Pa パ  12497  Hi ヒ  12498
        |Bi ビ  12499  Pi ピ  12500  Hu フ  12501  Bu ブ  12502  Pu プ  12503  He ヘ  12504
        |Be ベ  12505  Pe ペ  12506  Ho ホ  12507  Bo ボ  12508  Po ポ  12509  Ma マ  12510
        |Mi ミ  12511  Mu ム  12512  Me メ  12513  Mo モ  12514  YA ャ  12515  Ya ヤ  12516
        |YU ュ  12517  Yu ユ  12518  YO ョ  12519  Yo ヨ  12520  Ra ラ  12521  Ri リ  12522
        |Ru ル  12523  Re レ  12524  Ro ロ  12525  WA ヮ  12526  Wa ワ  12527  Wi ヰ  12528
        |We ヱ  12529  Wo ヲ  12530  N6 ン  12531  Vu ヴ  12532  KA ヵ  12533  KE ヶ  12534
        |Va ヷ  12535  Vi ヸ  12536  Ve ヹ  12537  Vo ヺ  12538  .6 ・  12539  -6 ー  12540
        |*6 ヽ  12541  +6 ヾ  12542  b4 ㄅ  12549  p4 ㄆ  12550  m4 ㄇ  12551  f4 ㄈ  12552
        |d4 ㄉ  12553  t4 ㄊ  12554  n4 ㄋ  12555  l4 ㄌ  12556  g4 ㄍ  12557  k4 ㄎ  12558
        |h4 ㄏ  12559  j4 ㄐ  12560  q4 ㄑ  12561  x4 ㄒ  12562  zh ㄓ  12563  ch ㄔ  12564
        |sh ㄕ  12565  r4 ㄖ  12566  z4 ㄗ  12567  c4 ㄘ  12568  s4 ㄙ  12569  a4 ㄚ  12570
        |o4 ㄛ  12571  e4 ㄜ  12572  ai ㄞ  12574  ei ㄟ  12575  au ㄠ  12576  ou ㄡ  12577
        |an ㄢ  12578  en ㄣ  12579  aN ㄤ  12580  eN ㄥ  12581  er ㄦ  12582  i4 ㄧ  12583
        |u4 ㄨ  12584  iu ㄩ  12585  v4 ㄪ  12586  nG ㄫ  12587  gn ㄬ  12588  1c ㈠  12832
        |2c ㈡  12833  3c ㈢  12834  4c ㈣  12835  5c ㈤  12836  6c ㈥  12837  7c ㈦  12838
        |8c ㈧  12839  9c ㈨  12840  ff ﬀ  64256  fi ﬁ  64257  fl ﬂ  64258  ft ﬅ  64261
        |st ﬆ  64262
      """.trimMargin()
    )
  }

  @Test
  fun `test digraph output with headers`() {
    assertCommandOutput(
      "digraphs!",
      """
        |NU ^@  10    SH ^A   1    SX ^B   2    EX ^C   3    ET ^D   4    EQ ^E   5
        |AK ^F   6    BL ^G   7    BS ^H   8    HT ^I   9    LF ^J  10    VT ^K  11
        |FF ^L  12    CR ^M  13    SO ^N  14    SI ^O  15    DL ^P  16    D1 ^Q  17
        |D2 ^R  18    D3 ^S  19    D4 ^T  20    NK ^U  21    SY ^V  22    EB ^W  23
        |CN ^X  24    EM ^Y  25    SB ^Z  26    EC ^[  27    FS ^\  28    GS ^]  29
        |RS ^^  30    US ^_  31    SP     32    Nb #   35    DO $   36    At @   64
        |<( [   91    // \   92    )> ]   93    '> ^   94    '! `   96    (! {  123
        |!! |  124    !) }  125    '? ~  126    DT ^? 127    PA <80> 128  HO <81> 129
        |BH <82> 130  NH <83> 131  IN <84> 132  NL <85> 133  SA <86> 134  ES <87> 135
        |HS <88> 136  HJ <89> 137  VS <8a> 138  PD <8b> 139  PU <8c> 140  RI <8d> 141
        |S2 <8e> 142  S3 <8f> 143  DC <90> 144  P1 <91> 145  P2 <92> 146  TS <93> 147
        |CC <94> 148  MW <95> 149  SG <96> 150  EG <97> 151  SS <98> 152  GC <99> 153
        |SC <9a> 154  CI <9b> 155  ST <9c> 156  OC <9d> 157  PM <9e> 158  AC <9f> 159
        |NS    160
        |Latin supplement
        |!I ¡  161    ~! ¡  161    Ct ¢  162    c| ¢  162    Pd £  163    $$ £  163
        |Cu ¤  164    ox ¤  164    Ye ¥  165    Y- ¥  165    BB ¦  166    || ¦  166
        |SE §  167    ': ¨  168    Co ©  169    cO ©  169    -a ª  170    << «  171
        |NO ¬  172    -, ¬  172    -- <ad> 173  Rg ®  174    'm ¯  175    -= ¯  175
        |DG °  176    ~o °  176    +- ±  177    2S ²  178    22 ²  178    3S ³  179
        |33 ³  179    '' ´  180    My µ  181    PI ¶  182    pp ¶  182    .M ·  183
        |~. ·  183    ', ¸  184    1S ¹  185    11 ¹  185    -o º  186    >> »  187
        |14 ¼  188    12 ½  189    34 ¾  190    ?I ¿  191    ~? ¿  191    A! À  192
        |A` À  192    A' Á  193    A> Â  194    A^ Â  194    A? Ã  195    A~ Ã  195
        |A: Ä  196    A" Ä  196    AA Å  197    A@ Å  197    AE Æ  198    C, Ç  199
        |E! È  200    E` È  200    E' É  201    E> Ê  202    E^ Ê  202    E: Ë  203
        |E" Ë  203    I! Ì  204    I` Ì  204    I' Í  205    I> Î  206    I^ Î  206
        |I: Ï  207    I" Ï  207    D- Ð  208    N? Ñ  209    N~ Ñ  209    O! Ò  210
        |O` Ò  210    O' Ó  211    O> Ô  212    O^ Ô  212    O? Õ  213    O~ Õ  213
        |O: Ö  214    *X ×  215    /\ ×  215    O/ Ø  216    U! Ù  217    U` Ù  217
        |U' Ú  218    U> Û  219    U^ Û  219    U: Ü  220    Y' Ý  221    TH Þ  222
        |Ip Þ  222    ss ß  223    a! à  224    a` à  224    a' á  225    a> â  226
        |a^ â  226    a? ã  227    a~ ã  227    a: ä  228    a" ä  228    aa å  229
        |a@ å  229    ae æ  230    c, ç  231    e! è  232    e` è  232    e' é  233
        |e> ê  234    e^ ê  234    e: ë  235    e" ë  235    i! ì  236    i` ì  236
        |i' í  237    i> î  238    i^ î  238    i: ï  239    d- ð  240    n? ñ  241
        |n~ ñ  241    o! ò  242    o` ò  242    o' ó  243    o> ô  244    o^ ô  244
        |o? õ  245    o~ õ  245    o: ö  246    -: ÷  247    o/ ø  248    u! ù  249
        |u` ù  249    u' ú  250    u> û  251    u^ û  251    u: ü  252    y' ý  253
        |th þ  254    y: ÿ  255    y" ÿ  255    A- Ā  256    a- ā  257    A( Ă  258
        |a( ă  259    A; Ą  260    a; ą  261    C' Ć  262    c' ć  263    C> Ĉ  264
        |c> ĉ  265    C. Ċ  266    c. ċ  267    C< Č  268    c< č  269    D< Ď  270
        |d< ď  271    D/ Đ  272    d/ đ  273    E- Ē  274    e- ē  275    E( Ĕ  276
        |e( ĕ  277    E. Ė  278    e. ė  279    E; Ę  280    e; ę  281    E< Ě  282
        |e< ě  283    G> Ĝ  284    g> ĝ  285    G( Ğ  286    g( ğ  287    G. Ġ  288
        |g. ġ  289    G, Ģ  290    g, ģ  291    H> Ĥ  292    h> ĥ  293    H/ Ħ  294
        |h/ ħ  295    I? Ĩ  296    i? ĩ  297    I- Ī  298    i- ī  299    I( Ĭ  300
        |i( ĭ  301    I; Į  302    i; į  303    I. İ  304    i. ı  305    IJ Ĳ  306
        |ij ĳ  307    J> Ĵ  308    j> ĵ  309    K, Ķ  310    k, ķ  311    kk ĸ  312
        |L' Ĺ  313    l' ĺ  314    L, Ļ  315    l, ļ  316    L< Ľ  317    l< ľ  318
        |L. Ŀ  319    l. ŀ  320    L/ Ł  321    l/ ł  322    N' Ń  323    n' ń  324
        |N, Ņ  325    n, ņ  326    N< Ň  327    n< ň  328    'n ŉ  329    NG Ŋ  330
        |ng ŋ  331    O- Ō  332    o- ō  333    O( Ŏ  334    o( ŏ  335    O" Ő  336
        |o" ő  337    OE Œ  338    oe œ  339    R' Ŕ  340    r' ŕ  341    R, Ŗ  342
        |r, ŗ  343    R< Ř  344    r< ř  345    S' Ś  346    s' ś  347    S> Ŝ  348
        |s> ŝ  349    S, Ş  350    s, ş  351    S< Š  352    s< š  353    T, Ţ  354
        |t, ţ  355    T< Ť  356    t< ť  357    T/ Ŧ  358    t/ ŧ  359    U? Ũ  360
        |u? ũ  361    U- Ū  362    u- ū  363    U( Ŭ  364    u( ŭ  365    U0 Ů  366
        |u0 ů  367    U" Ű  368    u" ű  369    U; Ų  370    u; ų  371    W> Ŵ  372
        |w> ŵ  373    Y> Ŷ  374    y> ŷ  375    Y: Ÿ  376    Z' Ź  377    z' ź  378
        |Z. Ż  379    z. ż  380    Z< Ž  381    z< ž  382    O9 Ơ  416    o9 ơ  417
        |OI Ƣ  418    oi ƣ  419    yr Ʀ  422    U9 Ư  431    u9 ư  432    Z/ Ƶ  437
        |z/ ƶ  438    ED Ʒ  439    A< Ǎ  461    a< ǎ  462    I< Ǐ  463    i< ǐ  464
        |O< Ǒ  465    o< ǒ  466    U< Ǔ  467    u< ǔ  468    A1 Ǟ  478    a1 ǟ  479
        |A7 Ǡ  480    a7 ǡ  481    A3 Ǣ  482    a3 ǣ  483    G/ Ǥ  484    g/ ǥ  485
        |G< Ǧ  486    g< ǧ  487    K< Ǩ  488    k< ǩ  489    O; Ǫ  490    o; ǫ  491
        |O1 Ǭ  492    o1 ǭ  493    EZ Ǯ  494    ez ǯ  495    j< ǰ  496    G' Ǵ  500
        |g' ǵ  501    ;S ʿ  703    '< ˇ  711    '( ˘  728    '. ˙  729    '0 ˚  730
        |'; ˛  731    '" ˝  733
        |Greek and Coptic
        |A% Ά  902    E% Έ  904    Y% Ή  905    I% Ί  906    O% Ό  908    U% Ύ  910
        |W% Ώ  911    i3 ΐ  912    A* Α  913    B* Β  914    G* Γ  915    D* Δ  916
        |E* Ε  917    Z* Ζ  918    Y* Η  919    H* Θ  920    I* Ι  921    K* Κ  922
        |L* Λ  923    M* Μ  924    N* Ν  925    C* Ξ  926    O* Ο  927    P* Π  928
        |R* Ρ  929    S* Σ  931    T* Τ  932    U* Υ  933    F* Φ  934    X* Χ  935
        |Q* Ψ  936    W* Ω  937    J* Ϊ  938    V* Ϋ  939    a% ά  940    e% έ  941
        |y% ή  942    i% ί  943    u3 ΰ  944    a* α  945    b* β  946    g* γ  947
        |d* δ  948    e* ε  949    z* ζ  950    y* η  951    h* θ  952    i* ι  953
        |k* κ  954    l* λ  955    m* μ  956    n* ν  957    c* ξ  958    o* ο  959
        |p* π  960    r* ρ  961    *s ς  962    s* σ  963    t* τ  964    u* υ  965
        |f* φ  966    x* χ  967    q* ψ  968    w* ω  969    j* ϊ  970    v* ϋ  971
        |o% ό  972    u% ύ  973    w% ώ  974    'G Ϙ  984    ,G ϙ  985    T3 Ϛ  986
        |t3 ϛ  987    M3 Ϝ  988    m3 ϝ  989    K3 Ϟ  990    k3 ϟ  991    P3 Ϡ  992
        |p3 ϡ  993    '% ϴ  1012   j3 ϵ  1013
        |Cyrillic
        |IO Ё  1025   D% Ђ  1026   G% Ѓ  1027   IE Є  1028   DS Ѕ  1029   II І  1030
        |YI Ї  1031   J% Ј  1032   LJ Љ  1033   NJ Њ  1034   Ts Ћ  1035   KJ Ќ  1036
        |V% Ў  1038   DZ Џ  1039   A= А  1040   B= Б  1041   V= В  1042   G= Г  1043
        |D= Д  1044   E= Е  1045   Z% Ж  1046   Z= З  1047   I= И  1048   J= Й  1049
        |K= К  1050   L= Л  1051   M= М  1052   N= Н  1053   O= О  1054   P= П  1055
        |R= Р  1056   S= С  1057   T= Т  1058   U= У  1059   F= Ф  1060   H= Х  1061
        |C= Ц  1062   C% Ч  1063   S% Ш  1064   Sc Щ  1065   =" Ъ  1066   Y= Ы  1067
        |%" Ь  1068   JE Э  1069   JU Ю  1070   JA Я  1071   a= а  1072   b= б  1073
        |v= в  1074   g= г  1075   d= д  1076   e= е  1077   z% ж  1078   z= з  1079
        |i= и  1080   j= й  1081   k= к  1082   l= л  1083   m= м  1084   n= н  1085
        |o= о  1086   p= п  1087   r= р  1088   s= с  1089   t= т  1090   u= у  1091
        |f= ф  1092   h= х  1093   c= ц  1094   c% ч  1095   s% ш  1096   sc щ  1097
        |=' ъ  1098   y= ы  1099   %' ь  1100   je э  1101   ju ю  1102   ja я  1103
        |io ё  1105   d% ђ  1106   g% ѓ  1107   ie є  1108   ds ѕ  1109   ii і  1110
        |yi ї  1111   j% ј  1112   lj љ  1113   nj њ  1114   ts ћ  1115   kj ќ  1116
        |v% ў  1118   dz џ  1119   Y3 Ѣ  1122   y3 ѣ  1123   O3 Ѫ  1130   o3 ѫ  1131
        |F3 Ѳ  1138   f3 ѳ  1139   V3 Ѵ  1140   v3 ѵ  1141   C3 Ҁ  1152   c3 ҁ  1153
        |G3 Ґ  1168   g3 ґ  1169
        |Hebrew
        |A+ ⁧א⁩  1488   B+ ⁧ב⁩  1489   G+ ⁧ג⁩  1490   D+ ⁧ד⁩  1491   H+ ⁧ה⁩  1492   W+ ⁧ו⁩  1493
        |Z+ ⁧ז⁩  1494   X+ ⁧ח⁩  1495   Tj ⁧ט⁩  1496   J+ ⁧י⁩  1497   K% ⁧ך⁩  1498   K+ ⁧כ⁩  1499
        |L+ ⁧ל⁩  1500   M% ⁧ם⁩  1501   M+ ⁧מ⁩  1502   N% ⁧ן⁩  1503   N+ ⁧נ⁩  1504   S+ ⁧ס⁩  1505
        |E+ ⁧ע⁩  1506   P% ⁧ף⁩  1507   P+ ⁧פ⁩  1508   Zj ⁧ץ⁩  1509   ZJ ⁧צ⁩  1510   Q+ ⁧ק⁩  1511
        |R+ ⁧ר⁩  1512   Sh ⁧ש⁩  1513   T+ ⁧ת⁩  1514
        |Arabic
        |,+ ،  1548   ;+ ⁧؛⁩  1563   ?+ ⁧؟⁩  1567   H' ⁧ء⁩  1569   aM ⁧آ⁩  1570   aH ⁧أ⁩  1571
        |wH ⁧ؤ⁩  1572   ah ⁧إ⁩  1573   yH ⁧ئ⁩  1574   a+ ⁧ا⁩  1575   b+ ⁧ب⁩  1576   tm ⁧ة⁩  1577
        |t+ ⁧ت⁩  1578   tk ⁧ث⁩  1579   g+ ⁧ج⁩  1580   hk ⁧ح⁩  1581   x+ ⁧خ⁩  1582   d+ ⁧د⁩  1583
        |dk ⁧ذ⁩  1584   r+ ⁧ر⁩  1585   z+ ⁧ز⁩  1586   s+ ⁧س⁩  1587   sn ⁧ش⁩  1588   c+ ⁧ص⁩  1589
        |dd ⁧ض⁩  1590   tj ⁧ط⁩  1591   zH ⁧ظ⁩  1592   e+ ⁧ع⁩  1593   i+ ⁧غ⁩  1594   ++ ⁧ـ⁩  1600
        |f+ ⁧ف⁩  1601   q+ ⁧ق⁩  1602   k+ ⁧ك⁩  1603   l+ ⁧ل⁩  1604   m+ ⁧م⁩  1605   n+ ⁧ن⁩  1606
        |h+ ⁧ه⁩  1607   w+ ⁧و⁩  1608   j+ ⁧ى⁩  1609   y+ ⁧ي⁩  1610   :+  ً  1611   "+  ٌ  1612
        |=+  ٍ  1613   /+  َ  1614   '+  ُ  1615   1+  ِ  1616   3+  ّ  1617   0+  ْ  1618
        |aS  ٰ  1648   p+ ⁧پ⁩  1662   v+ ⁧ڤ⁩  1700   gf ⁧گ⁩  1711   0a ۰  1776   1a ۱  1777
        |2a ۲  1778   3a ۳  1779   4a ۴  1780   5a ۵  1781   6a ۶  1782   7a ۷  1783
        |8a ۸  1784   9a ۹  1785
        |Latin extended
        |B. Ḃ  7682   b. ḃ  7683   B_ Ḇ  7686   b_ ḇ  7687   D. Ḋ  7690   d. ḋ  7691
        |D_ Ḏ  7694   d_ ḏ  7695   D, Ḑ  7696   d, ḑ  7697   F. Ḟ  7710   f. ḟ  7711
        |G- Ḡ  7712   g- ḡ  7713   H. Ḣ  7714   h. ḣ  7715   H: Ḧ  7718   h: ḧ  7719
        |H, Ḩ  7720   h, ḩ  7721   K' Ḱ  7728   k' ḱ  7729   K_ Ḵ  7732   k_ ḵ  7733
        |L_ Ḻ  7738   l_ ḻ  7739   M' Ḿ  7742   m' ḿ  7743   M. Ṁ  7744   m. ṁ  7745
        |N. Ṅ  7748   n. ṅ  7749   N_ Ṉ  7752   n_ ṉ  7753   P' Ṕ  7764   p' ṕ  7765
        |P. Ṗ  7766   p. ṗ  7767   R. Ṙ  7768   r. ṙ  7769   R_ Ṟ  7774   r_ ṟ  7775
        |S. Ṡ  7776   s. ṡ  7777   T. Ṫ  7786   t. ṫ  7787   T_ Ṯ  7790   t_ ṯ  7791
        |V? Ṽ  7804   v? ṽ  7805   W! Ẁ  7808   W` Ẁ  7808   w! ẁ  7809   w` ẁ  7809
        |W' Ẃ  7810   w' ẃ  7811   W: Ẅ  7812   w: ẅ  7813   W. Ẇ  7814   w. ẇ  7815
        |X. Ẋ  7818   x. ẋ  7819   X: Ẍ  7820   x: ẍ  7821   Y. Ẏ  7822   y. ẏ  7823
        |Z> Ẑ  7824   z> ẑ  7825   Z_ Ẕ  7828   z_ ẕ  7829   h_ ẖ  7830   t: ẗ  7831
        |w0 ẘ  7832   y0 ẙ  7833   A2 Ả  7842   a2 ả  7843   E2 Ẻ  7866   e2 ẻ  7867
        |E? Ẽ  7868   e? ẽ  7869   I2 Ỉ  7880   i2 ỉ  7881   O2 Ỏ  7886   o2 ỏ  7887
        |U2 Ủ  7910   u2 ủ  7911   Y! Ỳ  7922   Y` Ỳ  7922   y! ỳ  7923   y` ỳ  7923
        |Y2 Ỷ  7926   y2 ỷ  7927   Y? Ỹ  7928   y? ỹ  7929
        |Greek extended
        |;' ἀ  7936   ,' ἁ  7937   ;! ἂ  7938   ,! ἃ  7939   ?; ἄ  7940   ?, ἅ  7941
        |!: ἆ  7942   ?: ἇ  7943
        |Punctuation
        |1N    8194   1M    8195   3M    8196   4M    8197   6M    8198   1T    8201
        |1H    8202   -1 ‐  8208   -N –  8211   -M —  8212   -3 ―  8213   !2 ‖  8214
        |=2 ‗  8215   '6 ‘  8216   '9 ’  8217   .9 ‚  8218   9' ‛  8219   "6 “  8220
        |"9 ”  8221   :9 „  8222   9" ‟  8223   /- †  8224   /= ‡  8225   oo •  8226
        |.. ‥  8229   ,. …  8230   %0 ‰  8240   1' ′  8242   2' ″  8243   3' ‴  8244
        |4' ⁗  8279   1" ‵  8245   2" ‶  8246   3" ‷  8247   Ca ‸  8248   <1 ‹  8249
        |>1 ›  8250   :X ※  8251   '- ‾  8254   /f ⁄  8260
        |Super- and subscripts
        |0S ⁰  8304   4S ⁴  8308   5S ⁵  8309   6S ⁶  8310   7S ⁷  8311   8S ⁸  8312
        |9S ⁹  8313   +S ⁺  8314   -S ⁻  8315   =S ⁼  8316   (S ⁽  8317   )S ⁾  8318
        |nS ⁿ  8319   0s ₀  8320   1s ₁  8321   2s ₂  8322   3s ₃  8323   4s ₄  8324
        |5s ₅  8325   6s ₆  8326   7s ₇  8327   8s ₈  8328   9s ₉  8329   +s ₊  8330
        |-s ₋  8331   =s ₌  8332   (s ₍  8333   )s ₎  8334
        |Currency
        |Li ₤  8356   Pt ₧  8359   W= ₩  8361   =e €  8364   Eu €  8364   =R ₽  8381
        |=P ₽  8381
        |Other
        |oC ℃  8451   co ℅  8453   oF ℉  8457   N0 №  8470   PO ℗  8471   Rx ℞  8478
        |SM ℠  8480   TM ™  8482   Om Ω  8486   AO Å  8491   13 ⅓  8531   23 ⅔  8532
        |15 ⅕  8533   25 ⅖  8534   35 ⅗  8535   45 ⅘  8536   16 ⅙  8537   56 ⅚  8538
        |18 ⅛  8539   38 ⅜  8540   58 ⅝  8541   78 ⅞  8542
        |Roman numbers
        |1R Ⅰ  8544   2R Ⅱ  8545   3R Ⅲ  8546   4R Ⅳ  8547   5R Ⅴ  8548   6R Ⅵ  8549
        |7R Ⅶ  8550   8R Ⅷ  8551   9R Ⅸ  8552   aR Ⅹ  8553   bR Ⅺ  8554   cR Ⅻ  8555
        |1r ⅰ  8560   2r ⅱ  8561   3r ⅲ  8562   4r ⅳ  8563   5r ⅴ  8564   6r ⅵ  8565
        |7r ⅶ  8566   8r ⅷ  8567   9r ⅸ  8568   ar ⅹ  8569   br ⅺ  8570   cr ⅻ  8571
        |Arrows
        |<- ←  8592   -! ↑  8593   -> →  8594   -v ↓  8595   <> ↔  8596   UD ↕  8597
        |<= ⇐  8656   => ⇒  8658   == ⇔  8660
        |Mathematical operators
        |FA ∀  8704   dP ∂  8706   TE ∃  8707   /0 ∅  8709   DE ∆  8710   NB ∇  8711
        |(- ∈  8712   -) ∋  8715   *P ∏  8719   +Z ∑  8721   -2 −  8722   -+ ∓  8723
        |*- ∗  8727   Ob ∘  8728   Sb ∙  8729   RT √  8730   0( ∝  8733   00 ∞  8734
        |-L ∟  8735   -V ∠  8736   PP ∥  8741   AN ∧  8743   OR ∨  8744   (U ∩  8745
        |)U ∪  8746   In ∫  8747   DI ∬  8748   Io ∮  8750   .: ∴  8756   :. ∵  8757
        |:R ∶  8758   :: ∷  8759   ?1 ∼  8764   CG ∾  8766   ?- ≃  8771   ?= ≅  8773
        |?2 ≈  8776   =? ≌  8780   HI ≓  8787   != ≠  8800   =3 ≡  8801   =< ≤  8804
        |>= ≥  8805   <* ≪  8810   *> ≫  8811   !< ≮  8814   !> ≯  8815   (C ⊂  8834
        |)C ⊃  8835   (_ ⊆  8838   )_ ⊇  8839   0. ⊙  8857   02 ⊚  8858   -T ⊥  8869
        |.P ⋅  8901   :3 ⋮  8942   .3 ⋯  8943
        |Technical
        |Eh ⌂  8962   <7 ⌈  8968   >7 ⌉  8969   7< ⌊  8970   7> ⌋  8971   NI ⌐  8976
        |(A ⌒  8978   TR ⌕  8981   Iu ⌠  8992   Il ⌡  8993   </ 〈  9001   /> 〉  9002
        |Other
        |Vs ␣  9251   1h ⑀  9280   3h ⑁  9281   2h ⑂  9282   4h ⑃  9283   1j ⑆  9286
        |2j ⑇  9287   3j ⑈  9288   4j ⑉  9289   1. ⒈  9352   2. ⒉  9353   3. ⒊  9354
        |4. ⒋  9355   5. ⒌  9356   6. ⒍  9357   7. ⒎  9358   8. ⒏  9359   9. ⒐  9360
        |Box drawing
        |hh ─  9472   HH ━  9473   vv │  9474   VV ┃  9475   3- ┄  9476   3_ ┅  9477
        |3! ┆  9478   3/ ┇  9479   4- ┈  9480   4_ ┉  9481   4! ┊  9482   4/ ┋  9483
        |dr ┌  9484   dR ┍  9485   Dr ┎  9486   DR ┏  9487   dl ┐  9488   dL ┑  9489
        |Dl ┒  9490   LD ┓  9491   ur └  9492   uR ┕  9493   Ur ┖  9494   UR ┗  9495
        |ul ┘  9496   uL ┙  9497   Ul ┚  9498   UL ┛  9499   vr ├  9500   vR ┝  9501
        |Vr ┠  9504   VR ┣  9507   vl ┤  9508   vL ┥  9509   Vl ┨  9512   VL ┫  9515
        |dh ┬  9516   dH ┯  9519   Dh ┰  9520   DH ┳  9523   uh ┴  9524   uH ┷  9527
        |Uh ┸  9528   UH ┻  9531   vh ┼  9532   vH ┿  9535   Vh ╂  9538   VH ╋  9547
        |FD ╱  9585   BD ╲  9586
        |Block elements
        |TB ▀  9600   LB ▄  9604   FB █  9608   lB ▌  9612   RB ▐  9616   .S ░  9617
        |:S ▒  9618   ?S ▓  9619
        |Geometric shapes
        |fS ■  9632   OS □  9633   RO ▢  9634   Rr ▣  9635   RF ▤  9636   RY ▥  9637
        |RH ▦  9638   RZ ▧  9639   RK ▨  9640   RX ▩  9641   sB ▪  9642   SR ▬  9644
        |Or ▭  9645   UT ▲  9650   uT △  9651   PR ▶  9654   Tr ▷  9655   Dt ▼  9660
        |dT ▽  9661   PL ◀  9664   Tl ◁  9665   Db ◆  9670   Dw ◇  9671   LZ ◊  9674
        |0m ○  9675   0o ◎  9678   0M ●  9679   0L ◐  9680   0R ◑  9681   Sn ◘  9688
        |Ic ◙  9689   Fd ◢  9698   Bd ◣  9699
        |Symbols
        |*2 ★  9733   *1 ☆  9734   <H ☜  9756   >H ☞  9758   0u ☺  9786   0U ☻  9787
        |SU ☼  9788   Fm ♀  9792   Ml ♂  9794   cS ♠  9824   cH ♡  9825   cD ♢  9826
        |cC ♣  9827   Md ♩  9833   M8 ♪  9834   M2 ♫  9835   Mb ♭  9837   Mx ♮  9838
        |MX ♯  9839
        |Dingbats
        |OK ✓  10003  XX ✗  10007  -X ✠  10016
        |CJK symbols and punctuation
        |IS 　  12288  ,_ 、  12289  ._ 。  12290  +" 〃  12291  +_ 〄  12292  *_ 々  12293
        |;_ 〆  12294  0_ 〇  12295  <+ 《  12298  >+ 》  12299  <' 「  12300  >' 」  12301
        |<" 『  12302  >" 』  12303  (" 【  12304  )" 】  12305  =T 〒  12306  =_ 〓  12307
        |(' 〔  12308  )' 〕  12309  (I 〖  12310  )I 〗  12311  -? 〜  12316
        |Hiragana
        |A5 ぁ  12353  a5 あ  12354  I5 ぃ  12355  i5 い  12356  U5 ぅ  12357  u5 う  12358
        |E5 ぇ  12359  e5 え  12360  O5 ぉ  12361  o5 お  12362  ka か  12363  ga が  12364
        |ki き  12365  gi ぎ  12366  ku く  12367  gu ぐ  12368  ke け  12369  ge げ  12370
        |ko こ  12371  go ご  12372  sa さ  12373  za ざ  12374  si し  12375  zi じ  12376
        |su す  12377  zu ず  12378  se せ  12379  ze ぜ  12380  so そ  12381  zo ぞ  12382
        |ta た  12383  da だ  12384  ti ち  12385  di ぢ  12386  tU っ  12387  tu つ  12388
        |du づ  12389  te て  12390  de で  12391  to と  12392  do ど  12393  na な  12394
        |ni に  12395  nu ぬ  12396  ne ね  12397  no の  12398  ha は  12399  ba ば  12400
        |pa ぱ  12401  hi ひ  12402  bi び  12403  pi ぴ  12404  hu ふ  12405  bu ぶ  12406
        |pu ぷ  12407  he へ  12408  be べ  12409  pe ぺ  12410  ho ほ  12411  bo ぼ  12412
        |po ぽ  12413  ma ま  12414  mi み  12415  mu む  12416  me め  12417  mo も  12418
        |yA ゃ  12419  ya や  12420  yU ゅ  12421  yu ゆ  12422  yO ょ  12423  yo よ  12424
        |ra ら  12425  ri り  12426  ru る  12427  re れ  12428  ro ろ  12429  wA ゎ  12430
        |wa わ  12431  wi ゐ  12432  we ゑ  12433  wo を  12434  n5 ん  12435  vu ゔ  12436
        |"5 ゛  12443  05 ゜  12444  *5 ゝ  12445  +5 ゞ  12446
        |Katakana
        |a6 ァ  12449  A6 ア  12450  i6 ィ  12451  I6 イ  12452  u6 ゥ  12453  U6 ウ  12454
        |e6 ェ  12455  E6 エ  12456  o6 ォ  12457  O6 オ  12458  Ka カ  12459  Ga ガ  12460
        |Ki キ  12461  Gi ギ  12462  Ku ク  12463  Gu グ  12464  Ke ケ  12465  Ge ゲ  12466
        |Ko コ  12467  Go ゴ  12468  Sa サ  12469  Za ザ  12470  Si シ  12471  Zi ジ  12472
        |Su ス  12473  Zu ズ  12474  Se セ  12475  Ze ゼ  12476  So ソ  12477  Zo ゾ  12478
        |Ta タ  12479  Da ダ  12480  Ti チ  12481  Di ヂ  12482  TU ッ  12483  Tu ツ  12484
        |Du ヅ  12485  Te テ  12486  De デ  12487  To ト  12488  Do ド  12489  Na ナ  12490
        |Ni ニ  12491  Nu ヌ  12492  Ne ネ  12493  No ノ  12494  Ha ハ  12495  Ba バ  12496
        |Pa パ  12497  Hi ヒ  12498  Bi ビ  12499  Pi ピ  12500  Hu フ  12501  Bu ブ  12502
        |Pu プ  12503  He ヘ  12504  Be ベ  12505  Pe ペ  12506  Ho ホ  12507  Bo ボ  12508
        |Po ポ  12509  Ma マ  12510  Mi ミ  12511  Mu ム  12512  Me メ  12513  Mo モ  12514
        |YA ャ  12515  Ya ヤ  12516  YU ュ  12517  Yu ユ  12518  YO ョ  12519  Yo ヨ  12520
        |Ra ラ  12521  Ri リ  12522  Ru ル  12523  Re レ  12524  Ro ロ  12525  WA ヮ  12526
        |Wa ワ  12527  Wi ヰ  12528  We ヱ  12529  Wo ヲ  12530  N6 ン  12531  Vu ヴ  12532
        |KA ヵ  12533  KE ヶ  12534  Va ヷ  12535  Vi ヸ  12536  Ve ヹ  12537  Vo ヺ  12538
        |.6 ・  12539  -6 ー  12540  *6 ヽ  12541  +6 ヾ  12542
        |Bopomofo
        |b4 ㄅ  12549  p4 ㄆ  12550  m4 ㄇ  12551  f4 ㄈ  12552  d4 ㄉ  12553  t4 ㄊ  12554
        |n4 ㄋ  12555  l4 ㄌ  12556  g4 ㄍ  12557  k4 ㄎ  12558  h4 ㄏ  12559  j4 ㄐ  12560
        |q4 ㄑ  12561  x4 ㄒ  12562  zh ㄓ  12563  ch ㄔ  12564  sh ㄕ  12565  r4 ㄖ  12566
        |z4 ㄗ  12567  c4 ㄘ  12568  s4 ㄙ  12569  a4 ㄚ  12570  o4 ㄛ  12571  e4 ㄜ  12572
        |ai ㄞ  12574  ei ㄟ  12575  au ㄠ  12576  ou ㄡ  12577  an ㄢ  12578  en ㄣ  12579
        |aN ㄤ  12580  eN ㄥ  12581  er ㄦ  12582  i4 ㄧ  12583  u4 ㄨ  12584  iu ㄩ  12585
        |v4 ㄪ  12586  nG ㄫ  12587  gn ㄬ  12588
        |Other
        |1c ㈠  12832  2c ㈡  12833  3c ㈢  12834  4c ㈣  12835  5c ㈤  12836  6c ㈥  12837
        |7c ㈦  12838  8c ㈧  12839  9c ㈨  12840  ff ﬀ  64256  fi ﬁ  64257  fl ﬂ  64258
        |ft ﬅ  64261  st ﬆ  64262
      """.trimMargin()
    )
  }

  @Test
  fun `test digraph output with custom digraphs output in entered order`() {
    enterCommand("digraphs (0 9450 (2 9313 (1 9312")
    assertCommandOutput(
      "digraphs",
      """
        |NU ^@  10    SH ^A   1    SX ^B   2    EX ^C   3    ET ^D   4    EQ ^E   5
        |AK ^F   6    BL ^G   7    BS ^H   8    HT ^I   9    LF ^J  10    VT ^K  11
        |FF ^L  12    CR ^M  13    SO ^N  14    SI ^O  15    DL ^P  16    D1 ^Q  17
        |D2 ^R  18    D3 ^S  19    D4 ^T  20    NK ^U  21    SY ^V  22    EB ^W  23
        |CN ^X  24    EM ^Y  25    SB ^Z  26    EC ^[  27    FS ^\  28    GS ^]  29
        |RS ^^  30    US ^_  31    SP     32    Nb #   35    DO $   36    At @   64
        |<( [   91    // \   92    )> ]   93    '> ^   94    '! `   96    (! {  123
        |!! |  124    !) }  125    '? ~  126    DT ^? 127    PA <80> 128  HO <81> 129
        |BH <82> 130  NH <83> 131  IN <84> 132  NL <85> 133  SA <86> 134  ES <87> 135
        |HS <88> 136  HJ <89> 137  VS <8a> 138  PD <8b> 139  PU <8c> 140  RI <8d> 141
        |S2 <8e> 142  S3 <8f> 143  DC <90> 144  P1 <91> 145  P2 <92> 146  TS <93> 147
        |CC <94> 148  MW <95> 149  SG <96> 150  EG <97> 151  SS <98> 152  GC <99> 153
        |SC <9a> 154  CI <9b> 155  ST <9c> 156  OC <9d> 157  PM <9e> 158  AC <9f> 159
        |NS    160    !I ¡  161    ~! ¡  161    Ct ¢  162    c| ¢  162    Pd £  163
        |$$ £  163    Cu ¤  164    ox ¤  164    Ye ¥  165    Y- ¥  165    BB ¦  166
        ||| ¦  166    SE §  167    ': ¨  168    Co ©  169    cO ©  169    -a ª  170
        |<< «  171    NO ¬  172    -, ¬  172    -- <ad> 173  Rg ®  174    'm ¯  175
        |-= ¯  175    DG °  176    ~o °  176    +- ±  177    2S ²  178    22 ²  178
        |3S ³  179    33 ³  179    '' ´  180    My µ  181    PI ¶  182    pp ¶  182
        |.M ·  183    ~. ·  183    ', ¸  184    1S ¹  185    11 ¹  185    -o º  186
        |>> »  187    14 ¼  188    12 ½  189    34 ¾  190    ?I ¿  191    ~? ¿  191
        |A! À  192    A` À  192    A' Á  193    A> Â  194    A^ Â  194    A? Ã  195
        |A~ Ã  195    A: Ä  196    A" Ä  196    AA Å  197    A@ Å  197    AE Æ  198
        |C, Ç  199    E! È  200    E` È  200    E' É  201    E> Ê  202    E^ Ê  202
        |E: Ë  203    E" Ë  203    I! Ì  204    I` Ì  204    I' Í  205    I> Î  206
        |I^ Î  206    I: Ï  207    I" Ï  207    D- Ð  208    N? Ñ  209    N~ Ñ  209
        |O! Ò  210    O` Ò  210    O' Ó  211    O> Ô  212    O^ Ô  212    O? Õ  213
        |O~ Õ  213    O: Ö  214    *X ×  215    /\ ×  215    O/ Ø  216    U! Ù  217
        |U` Ù  217    U' Ú  218    U> Û  219    U^ Û  219    U: Ü  220    Y' Ý  221
        |TH Þ  222    Ip Þ  222    ss ß  223    a! à  224    a` à  224    a' á  225
        |a> â  226    a^ â  226    a? ã  227    a~ ã  227    a: ä  228    a" ä  228
        |aa å  229    a@ å  229    ae æ  230    c, ç  231    e! è  232    e` è  232
        |e' é  233    e> ê  234    e^ ê  234    e: ë  235    e" ë  235    i! ì  236
        |i` ì  236    i' í  237    i> î  238    i^ î  238    i: ï  239    d- ð  240
        |n? ñ  241    n~ ñ  241    o! ò  242    o` ò  242    o' ó  243    o> ô  244
        |o^ ô  244    o? õ  245    o~ õ  245    o: ö  246    -: ÷  247    o/ ø  248
        |u! ù  249    u` ù  249    u' ú  250    u> û  251    u^ û  251    u: ü  252
        |y' ý  253    th þ  254    y: ÿ  255    y" ÿ  255    A- Ā  256    a- ā  257
        |A( Ă  258    a( ă  259    A; Ą  260    a; ą  261    C' Ć  262    c' ć  263
        |C> Ĉ  264    c> ĉ  265    C. Ċ  266    c. ċ  267    C< Č  268    c< č  269
        |D< Ď  270    d< ď  271    D/ Đ  272    d/ đ  273    E- Ē  274    e- ē  275
        |E( Ĕ  276    e( ĕ  277    E. Ė  278    e. ė  279    E; Ę  280    e; ę  281
        |E< Ě  282    e< ě  283    G> Ĝ  284    g> ĝ  285    G( Ğ  286    g( ğ  287
        |G. Ġ  288    g. ġ  289    G, Ģ  290    g, ģ  291    H> Ĥ  292    h> ĥ  293
        |H/ Ħ  294    h/ ħ  295    I? Ĩ  296    i? ĩ  297    I- Ī  298    i- ī  299
        |I( Ĭ  300    i( ĭ  301    I; Į  302    i; į  303    I. İ  304    i. ı  305
        |IJ Ĳ  306    ij ĳ  307    J> Ĵ  308    j> ĵ  309    K, Ķ  310    k, ķ  311
        |kk ĸ  312    L' Ĺ  313    l' ĺ  314    L, Ļ  315    l, ļ  316    L< Ľ  317
        |l< ľ  318    L. Ŀ  319    l. ŀ  320    L/ Ł  321    l/ ł  322    N' Ń  323
        |n' ń  324    N, Ņ  325    n, ņ  326    N< Ň  327    n< ň  328    'n ŉ  329
        |NG Ŋ  330    ng ŋ  331    O- Ō  332    o- ō  333    O( Ŏ  334    o( ŏ  335
        |O" Ő  336    o" ő  337    OE Œ  338    oe œ  339    R' Ŕ  340    r' ŕ  341
        |R, Ŗ  342    r, ŗ  343    R< Ř  344    r< ř  345    S' Ś  346    s' ś  347
        |S> Ŝ  348    s> ŝ  349    S, Ş  350    s, ş  351    S< Š  352    s< š  353
        |T, Ţ  354    t, ţ  355    T< Ť  356    t< ť  357    T/ Ŧ  358    t/ ŧ  359
        |U? Ũ  360    u? ũ  361    U- Ū  362    u- ū  363    U( Ŭ  364    u( ŭ  365
        |U0 Ů  366    u0 ů  367    U" Ű  368    u" ű  369    U; Ų  370    u; ų  371
        |W> Ŵ  372    w> ŵ  373    Y> Ŷ  374    y> ŷ  375    Y: Ÿ  376    Z' Ź  377
        |z' ź  378    Z. Ż  379    z. ż  380    Z< Ž  381    z< ž  382    O9 Ơ  416
        |o9 ơ  417    OI Ƣ  418    oi ƣ  419    yr Ʀ  422    U9 Ư  431    u9 ư  432
        |Z/ Ƶ  437    z/ ƶ  438    ED Ʒ  439    A< Ǎ  461    a< ǎ  462    I< Ǐ  463
        |i< ǐ  464    O< Ǒ  465    o< ǒ  466    U< Ǔ  467    u< ǔ  468    A1 Ǟ  478
        |a1 ǟ  479    A7 Ǡ  480    a7 ǡ  481    A3 Ǣ  482    a3 ǣ  483    G/ Ǥ  484
        |g/ ǥ  485    G< Ǧ  486    g< ǧ  487    K< Ǩ  488    k< ǩ  489    O; Ǫ  490
        |o; ǫ  491    O1 Ǭ  492    o1 ǭ  493    EZ Ǯ  494    ez ǯ  495    j< ǰ  496
        |G' Ǵ  500    g' ǵ  501    ;S ʿ  703    '< ˇ  711    '( ˘  728    '. ˙  729
        |'0 ˚  730    '; ˛  731    '" ˝  733    A% Ά  902    E% Έ  904    Y% Ή  905
        |I% Ί  906    O% Ό  908    U% Ύ  910    W% Ώ  911    i3 ΐ  912    A* Α  913
        |B* Β  914    G* Γ  915    D* Δ  916    E* Ε  917    Z* Ζ  918    Y* Η  919
        |H* Θ  920    I* Ι  921    K* Κ  922    L* Λ  923    M* Μ  924    N* Ν  925
        |C* Ξ  926    O* Ο  927    P* Π  928    R* Ρ  929    S* Σ  931    T* Τ  932
        |U* Υ  933    F* Φ  934    X* Χ  935    Q* Ψ  936    W* Ω  937    J* Ϊ  938
        |V* Ϋ  939    a% ά  940    e% έ  941    y% ή  942    i% ί  943    u3 ΰ  944
        |a* α  945    b* β  946    g* γ  947    d* δ  948    e* ε  949    z* ζ  950
        |y* η  951    h* θ  952    i* ι  953    k* κ  954    l* λ  955    m* μ  956
        |n* ν  957    c* ξ  958    o* ο  959    p* π  960    r* ρ  961    *s ς  962
        |s* σ  963    t* τ  964    u* υ  965    f* φ  966    x* χ  967    q* ψ  968
        |w* ω  969    j* ϊ  970    v* ϋ  971    o% ό  972    u% ύ  973    w% ώ  974
        |'G Ϙ  984    ,G ϙ  985    T3 Ϛ  986    t3 ϛ  987    M3 Ϝ  988    m3 ϝ  989
        |K3 Ϟ  990    k3 ϟ  991    P3 Ϡ  992    p3 ϡ  993    '% ϴ  1012   j3 ϵ  1013
        |IO Ё  1025   D% Ђ  1026   G% Ѓ  1027   IE Є  1028   DS Ѕ  1029   II І  1030
        |YI Ї  1031   J% Ј  1032   LJ Љ  1033   NJ Њ  1034   Ts Ћ  1035   KJ Ќ  1036
        |V% Ў  1038   DZ Џ  1039   A= А  1040   B= Б  1041   V= В  1042   G= Г  1043
        |D= Д  1044   E= Е  1045   Z% Ж  1046   Z= З  1047   I= И  1048   J= Й  1049
        |K= К  1050   L= Л  1051   M= М  1052   N= Н  1053   O= О  1054   P= П  1055
        |R= Р  1056   S= С  1057   T= Т  1058   U= У  1059   F= Ф  1060   H= Х  1061
        |C= Ц  1062   C% Ч  1063   S% Ш  1064   Sc Щ  1065   =" Ъ  1066   Y= Ы  1067
        |%" Ь  1068   JE Э  1069   JU Ю  1070   JA Я  1071   a= а  1072   b= б  1073
        |v= в  1074   g= г  1075   d= д  1076   e= е  1077   z% ж  1078   z= з  1079
        |i= и  1080   j= й  1081   k= к  1082   l= л  1083   m= м  1084   n= н  1085
        |o= о  1086   p= п  1087   r= р  1088   s= с  1089   t= т  1090   u= у  1091
        |f= ф  1092   h= х  1093   c= ц  1094   c% ч  1095   s% ш  1096   sc щ  1097
        |=' ъ  1098   y= ы  1099   %' ь  1100   je э  1101   ju ю  1102   ja я  1103
        |io ё  1105   d% ђ  1106   g% ѓ  1107   ie є  1108   ds ѕ  1109   ii і  1110
        |yi ї  1111   j% ј  1112   lj љ  1113   nj њ  1114   ts ћ  1115   kj ќ  1116
        |v% ў  1118   dz џ  1119   Y3 Ѣ  1122   y3 ѣ  1123   O3 Ѫ  1130   o3 ѫ  1131
        |F3 Ѳ  1138   f3 ѳ  1139   V3 Ѵ  1140   v3 ѵ  1141   C3 Ҁ  1152   c3 ҁ  1153
        |G3 Ґ  1168   g3 ґ  1169   A+ ⁧א⁩  1488   B+ ⁧ב⁩  1489   G+ ⁧ג⁩  1490   D+ ⁧ד⁩  1491
        |H+ ⁧ה⁩  1492   W+ ⁧ו⁩  1493   Z+ ⁧ז⁩  1494   X+ ⁧ח⁩  1495   Tj ⁧ט⁩  1496   J+ ⁧י⁩  1497
        |K% ⁧ך⁩  1498   K+ ⁧כ⁩  1499   L+ ⁧ל⁩  1500   M% ⁧ם⁩  1501   M+ ⁧מ⁩  1502   N% ⁧ן⁩  1503
        |N+ ⁧נ⁩  1504   S+ ⁧ס⁩  1505   E+ ⁧ע⁩  1506   P% ⁧ף⁩  1507   P+ ⁧פ⁩  1508   Zj ⁧ץ⁩  1509
        |ZJ ⁧צ⁩  1510   Q+ ⁧ק⁩  1511   R+ ⁧ר⁩  1512   Sh ⁧ש⁩  1513   T+ ⁧ת⁩  1514   ,+ ،  1548
        |;+ ⁧؛⁩  1563   ?+ ⁧؟⁩  1567   H' ⁧ء⁩  1569   aM ⁧آ⁩  1570   aH ⁧أ⁩  1571   wH ⁧ؤ⁩  1572
        |ah ⁧إ⁩  1573   yH ⁧ئ⁩  1574   a+ ⁧ا⁩  1575   b+ ⁧ب⁩  1576   tm ⁧ة⁩  1577   t+ ⁧ت⁩  1578
        |tk ⁧ث⁩  1579   g+ ⁧ج⁩  1580   hk ⁧ح⁩  1581   x+ ⁧خ⁩  1582   d+ ⁧د⁩  1583   dk ⁧ذ⁩  1584
        |r+ ⁧ر⁩  1585   z+ ⁧ز⁩  1586   s+ ⁧س⁩  1587   sn ⁧ش⁩  1588   c+ ⁧ص⁩  1589   dd ⁧ض⁩  1590
        |tj ⁧ط⁩  1591   zH ⁧ظ⁩  1592   e+ ⁧ع⁩  1593   i+ ⁧غ⁩  1594   ++ ⁧ـ⁩  1600   f+ ⁧ف⁩  1601
        |q+ ⁧ق⁩  1602   k+ ⁧ك⁩  1603   l+ ⁧ل⁩  1604   m+ ⁧م⁩  1605   n+ ⁧ن⁩  1606   h+ ⁧ه⁩  1607
        |w+ ⁧و⁩  1608   j+ ⁧ى⁩  1609   y+ ⁧ي⁩  1610   :+  ً  1611   "+  ٌ  1612   =+  ٍ  1613
        |/+  َ  1614   '+  ُ  1615   1+  ِ  1616   3+  ّ  1617   0+  ْ  1618   aS  ٰ  1648
        |p+ ⁧پ⁩  1662   v+ ⁧ڤ⁩  1700   gf ⁧گ⁩  1711   0a ۰  1776   1a ۱  1777   2a ۲  1778
        |3a ۳  1779   4a ۴  1780   5a ۵  1781   6a ۶  1782   7a ۷  1783   8a ۸  1784
        |9a ۹  1785   B. Ḃ  7682   b. ḃ  7683   B_ Ḇ  7686   b_ ḇ  7687   D. Ḋ  7690
        |d. ḋ  7691   D_ Ḏ  7694   d_ ḏ  7695   D, Ḑ  7696   d, ḑ  7697   F. Ḟ  7710
        |f. ḟ  7711   G- Ḡ  7712   g- ḡ  7713   H. Ḣ  7714   h. ḣ  7715   H: Ḧ  7718
        |h: ḧ  7719   H, Ḩ  7720   h, ḩ  7721   K' Ḱ  7728   k' ḱ  7729   K_ Ḵ  7732
        |k_ ḵ  7733   L_ Ḻ  7738   l_ ḻ  7739   M' Ḿ  7742   m' ḿ  7743   M. Ṁ  7744
        |m. ṁ  7745   N. Ṅ  7748   n. ṅ  7749   N_ Ṉ  7752   n_ ṉ  7753   P' Ṕ  7764
        |p' ṕ  7765   P. Ṗ  7766   p. ṗ  7767   R. Ṙ  7768   r. ṙ  7769   R_ Ṟ  7774
        |r_ ṟ  7775   S. Ṡ  7776   s. ṡ  7777   T. Ṫ  7786   t. ṫ  7787   T_ Ṯ  7790
        |t_ ṯ  7791   V? Ṽ  7804   v? ṽ  7805   W! Ẁ  7808   W` Ẁ  7808   w! ẁ  7809
        |w` ẁ  7809   W' Ẃ  7810   w' ẃ  7811   W: Ẅ  7812   w: ẅ  7813   W. Ẇ  7814
        |w. ẇ  7815   X. Ẋ  7818   x. ẋ  7819   X: Ẍ  7820   x: ẍ  7821   Y. Ẏ  7822
        |y. ẏ  7823   Z> Ẑ  7824   z> ẑ  7825   Z_ Ẕ  7828   z_ ẕ  7829   h_ ẖ  7830
        |t: ẗ  7831   w0 ẘ  7832   y0 ẙ  7833   A2 Ả  7842   a2 ả  7843   E2 Ẻ  7866
        |e2 ẻ  7867   E? Ẽ  7868   e? ẽ  7869   I2 Ỉ  7880   i2 ỉ  7881   O2 Ỏ  7886
        |o2 ỏ  7887   U2 Ủ  7910   u2 ủ  7911   Y! Ỳ  7922   Y` Ỳ  7922   y! ỳ  7923
        |y` ỳ  7923   Y2 Ỷ  7926   y2 ỷ  7927   Y? Ỹ  7928   y? ỹ  7929   ;' ἀ  7936
        |,' ἁ  7937   ;! ἂ  7938   ,! ἃ  7939   ?; ἄ  7940   ?, ἅ  7941   !: ἆ  7942
        |?: ἇ  7943   1N    8194   1M    8195   3M    8196   4M    8197   6M    8198
        |1T    8201   1H    8202   -1 ‐  8208   -N –  8211   -M —  8212   -3 ―  8213
        |!2 ‖  8214   =2 ‗  8215   '6 ‘  8216   '9 ’  8217   .9 ‚  8218   9' ‛  8219
        |"6 “  8220   "9 ”  8221   :9 „  8222   9" ‟  8223   /- †  8224   /= ‡  8225
        |oo •  8226   .. ‥  8229   ,. …  8230   %0 ‰  8240   1' ′  8242   2' ″  8243
        |3' ‴  8244   4' ⁗  8279   1" ‵  8245   2" ‶  8246   3" ‷  8247   Ca ‸  8248
        |<1 ‹  8249   >1 ›  8250   :X ※  8251   '- ‾  8254   /f ⁄  8260   0S ⁰  8304
        |4S ⁴  8308   5S ⁵  8309   6S ⁶  8310   7S ⁷  8311   8S ⁸  8312   9S ⁹  8313
        |+S ⁺  8314   -S ⁻  8315   =S ⁼  8316   (S ⁽  8317   )S ⁾  8318   nS ⁿ  8319
        |0s ₀  8320   1s ₁  8321   2s ₂  8322   3s ₃  8323   4s ₄  8324   5s ₅  8325
        |6s ₆  8326   7s ₇  8327   8s ₈  8328   9s ₉  8329   +s ₊  8330   -s ₋  8331
        |=s ₌  8332   (s ₍  8333   )s ₎  8334   Li ₤  8356   Pt ₧  8359   W= ₩  8361
        |=e €  8364   Eu €  8364   =R ₽  8381   =P ₽  8381   oC ℃  8451   co ℅  8453
        |oF ℉  8457   N0 №  8470   PO ℗  8471   Rx ℞  8478   SM ℠  8480   TM ™  8482
        |Om Ω  8486   AO Å  8491   13 ⅓  8531   23 ⅔  8532   15 ⅕  8533   25 ⅖  8534
        |35 ⅗  8535   45 ⅘  8536   16 ⅙  8537   56 ⅚  8538   18 ⅛  8539   38 ⅜  8540
        |58 ⅝  8541   78 ⅞  8542   1R Ⅰ  8544   2R Ⅱ  8545   3R Ⅲ  8546   4R Ⅳ  8547
        |5R Ⅴ  8548   6R Ⅵ  8549   7R Ⅶ  8550   8R Ⅷ  8551   9R Ⅸ  8552   aR Ⅹ  8553
        |bR Ⅺ  8554   cR Ⅻ  8555   1r ⅰ  8560   2r ⅱ  8561   3r ⅲ  8562   4r ⅳ  8563
        |5r ⅴ  8564   6r ⅵ  8565   7r ⅶ  8566   8r ⅷ  8567   9r ⅸ  8568   ar ⅹ  8569
        |br ⅺ  8570   cr ⅻ  8571   <- ←  8592   -! ↑  8593   -> →  8594   -v ↓  8595
        |<> ↔  8596   UD ↕  8597   <= ⇐  8656   => ⇒  8658   == ⇔  8660   FA ∀  8704
        |dP ∂  8706   TE ∃  8707   /0 ∅  8709   DE ∆  8710   NB ∇  8711   (- ∈  8712
        |-) ∋  8715   *P ∏  8719   +Z ∑  8721   -2 −  8722   -+ ∓  8723   *- ∗  8727
        |Ob ∘  8728   Sb ∙  8729   RT √  8730   0( ∝  8733   00 ∞  8734   -L ∟  8735
        |-V ∠  8736   PP ∥  8741   AN ∧  8743   OR ∨  8744   (U ∩  8745   )U ∪  8746
        |In ∫  8747   DI ∬  8748   Io ∮  8750   .: ∴  8756   :. ∵  8757   :R ∶  8758
        |:: ∷  8759   ?1 ∼  8764   CG ∾  8766   ?- ≃  8771   ?= ≅  8773   ?2 ≈  8776
        |=? ≌  8780   HI ≓  8787   != ≠  8800   =3 ≡  8801   =< ≤  8804   >= ≥  8805
        |<* ≪  8810   *> ≫  8811   !< ≮  8814   !> ≯  8815   (C ⊂  8834   )C ⊃  8835
        |(_ ⊆  8838   )_ ⊇  8839   0. ⊙  8857   02 ⊚  8858   -T ⊥  8869   .P ⋅  8901
        |:3 ⋮  8942   .3 ⋯  8943   Eh ⌂  8962   <7 ⌈  8968   >7 ⌉  8969   7< ⌊  8970
        |7> ⌋  8971   NI ⌐  8976   (A ⌒  8978   TR ⌕  8981   Iu ⌠  8992   Il ⌡  8993
        |</ 〈  9001   /> 〉  9002   Vs ␣  9251   1h ⑀  9280   3h ⑁  9281   2h ⑂  9282
        |4h ⑃  9283   1j ⑆  9286   2j ⑇  9287   3j ⑈  9288   4j ⑉  9289   1. ⒈  9352
        |2. ⒉  9353   3. ⒊  9354   4. ⒋  9355   5. ⒌  9356   6. ⒍  9357   7. ⒎  9358
        |8. ⒏  9359   9. ⒐  9360   hh ─  9472   HH ━  9473   vv │  9474   VV ┃  9475
        |3- ┄  9476   3_ ┅  9477   3! ┆  9478   3/ ┇  9479   4- ┈  9480   4_ ┉  9481
        |4! ┊  9482   4/ ┋  9483   dr ┌  9484   dR ┍  9485   Dr ┎  9486   DR ┏  9487
        |dl ┐  9488   dL ┑  9489   Dl ┒  9490   LD ┓  9491   ur └  9492   uR ┕  9493
        |Ur ┖  9494   UR ┗  9495   ul ┘  9496   uL ┙  9497   Ul ┚  9498   UL ┛  9499
        |vr ├  9500   vR ┝  9501   Vr ┠  9504   VR ┣  9507   vl ┤  9508   vL ┥  9509
        |Vl ┨  9512   VL ┫  9515   dh ┬  9516   dH ┯  9519   Dh ┰  9520   DH ┳  9523
        |uh ┴  9524   uH ┷  9527   Uh ┸  9528   UH ┻  9531   vh ┼  9532   vH ┿  9535
        |Vh ╂  9538   VH ╋  9547   FD ╱  9585   BD ╲  9586   TB ▀  9600   LB ▄  9604
        |FB █  9608   lB ▌  9612   RB ▐  9616   .S ░  9617   :S ▒  9618   ?S ▓  9619
        |fS ■  9632   OS □  9633   RO ▢  9634   Rr ▣  9635   RF ▤  9636   RY ▥  9637
        |RH ▦  9638   RZ ▧  9639   RK ▨  9640   RX ▩  9641   sB ▪  9642   SR ▬  9644
        |Or ▭  9645   UT ▲  9650   uT △  9651   PR ▶  9654   Tr ▷  9655   Dt ▼  9660
        |dT ▽  9661   PL ◀  9664   Tl ◁  9665   Db ◆  9670   Dw ◇  9671   LZ ◊  9674
        |0m ○  9675   0o ◎  9678   0M ●  9679   0L ◐  9680   0R ◑  9681   Sn ◘  9688
        |Ic ◙  9689   Fd ◢  9698   Bd ◣  9699   *2 ★  9733   *1 ☆  9734   <H ☜  9756
        |>H ☞  9758   0u ☺  9786   0U ☻  9787   SU ☼  9788   Fm ♀  9792   Ml ♂  9794
        |cS ♠  9824   cH ♡  9825   cD ♢  9826   cC ♣  9827   Md ♩  9833   M8 ♪  9834
        |M2 ♫  9835   Mb ♭  9837   Mx ♮  9838   MX ♯  9839   OK ✓  10003  XX ✗  10007
        |-X ✠  10016  IS 　  12288  ,_ 、  12289  ._ 。  12290  +" 〃  12291  +_ 〄  12292
        |*_ 々  12293  ;_ 〆  12294  0_ 〇  12295  <+ 《  12298  >+ 》  12299  <' 「  12300
        |>' 」  12301  <" 『  12302  >" 』  12303  (" 【  12304  )" 】  12305  =T 〒  12306
        |=_ 〓  12307  (' 〔  12308  )' 〕  12309  (I 〖  12310  )I 〗  12311  -? 〜  12316
        |A5 ぁ  12353  a5 あ  12354  I5 ぃ  12355  i5 い  12356  U5 ぅ  12357  u5 う  12358
        |E5 ぇ  12359  e5 え  12360  O5 ぉ  12361  o5 お  12362  ka か  12363  ga が  12364
        |ki き  12365  gi ぎ  12366  ku く  12367  gu ぐ  12368  ke け  12369  ge げ  12370
        |ko こ  12371  go ご  12372  sa さ  12373  za ざ  12374  si し  12375  zi じ  12376
        |su す  12377  zu ず  12378  se せ  12379  ze ぜ  12380  so そ  12381  zo ぞ  12382
        |ta た  12383  da だ  12384  ti ち  12385  di ぢ  12386  tU っ  12387  tu つ  12388
        |du づ  12389  te て  12390  de で  12391  to と  12392  do ど  12393  na な  12394
        |ni に  12395  nu ぬ  12396  ne ね  12397  no の  12398  ha は  12399  ba ば  12400
        |pa ぱ  12401  hi ひ  12402  bi び  12403  pi ぴ  12404  hu ふ  12405  bu ぶ  12406
        |pu ぷ  12407  he へ  12408  be べ  12409  pe ぺ  12410  ho ほ  12411  bo ぼ  12412
        |po ぽ  12413  ma ま  12414  mi み  12415  mu む  12416  me め  12417  mo も  12418
        |yA ゃ  12419  ya や  12420  yU ゅ  12421  yu ゆ  12422  yO ょ  12423  yo よ  12424
        |ra ら  12425  ri り  12426  ru る  12427  re れ  12428  ro ろ  12429  wA ゎ  12430
        |wa わ  12431  wi ゐ  12432  we ゑ  12433  wo を  12434  n5 ん  12435  vu ゔ  12436
        |"5 ゛  12443  05 ゜  12444  *5 ゝ  12445  +5 ゞ  12446  a6 ァ  12449  A6 ア  12450
        |i6 ィ  12451  I6 イ  12452  u6 ゥ  12453  U6 ウ  12454  e6 ェ  12455  E6 エ  12456
        |o6 ォ  12457  O6 オ  12458  Ka カ  12459  Ga ガ  12460  Ki キ  12461  Gi ギ  12462
        |Ku ク  12463  Gu グ  12464  Ke ケ  12465  Ge ゲ  12466  Ko コ  12467  Go ゴ  12468
        |Sa サ  12469  Za ザ  12470  Si シ  12471  Zi ジ  12472  Su ス  12473  Zu ズ  12474
        |Se セ  12475  Ze ゼ  12476  So ソ  12477  Zo ゾ  12478  Ta タ  12479  Da ダ  12480
        |Ti チ  12481  Di ヂ  12482  TU ッ  12483  Tu ツ  12484  Du ヅ  12485  Te テ  12486
        |De デ  12487  To ト  12488  Do ド  12489  Na ナ  12490  Ni ニ  12491  Nu ヌ  12492
        |Ne ネ  12493  No ノ  12494  Ha ハ  12495  Ba バ  12496  Pa パ  12497  Hi ヒ  12498
        |Bi ビ  12499  Pi ピ  12500  Hu フ  12501  Bu ブ  12502  Pu プ  12503  He ヘ  12504
        |Be ベ  12505  Pe ペ  12506  Ho ホ  12507  Bo ボ  12508  Po ポ  12509  Ma マ  12510
        |Mi ミ  12511  Mu ム  12512  Me メ  12513  Mo モ  12514  YA ャ  12515  Ya ヤ  12516
        |YU ュ  12517  Yu ユ  12518  YO ョ  12519  Yo ヨ  12520  Ra ラ  12521  Ri リ  12522
        |Ru ル  12523  Re レ  12524  Ro ロ  12525  WA ヮ  12526  Wa ワ  12527  Wi ヰ  12528
        |We ヱ  12529  Wo ヲ  12530  N6 ン  12531  Vu ヴ  12532  KA ヵ  12533  KE ヶ  12534
        |Va ヷ  12535  Vi ヸ  12536  Ve ヹ  12537  Vo ヺ  12538  .6 ・  12539  -6 ー  12540
        |*6 ヽ  12541  +6 ヾ  12542  b4 ㄅ  12549  p4 ㄆ  12550  m4 ㄇ  12551  f4 ㄈ  12552
        |d4 ㄉ  12553  t4 ㄊ  12554  n4 ㄋ  12555  l4 ㄌ  12556  g4 ㄍ  12557  k4 ㄎ  12558
        |h4 ㄏ  12559  j4 ㄐ  12560  q4 ㄑ  12561  x4 ㄒ  12562  zh ㄓ  12563  ch ㄔ  12564
        |sh ㄕ  12565  r4 ㄖ  12566  z4 ㄗ  12567  c4 ㄘ  12568  s4 ㄙ  12569  a4 ㄚ  12570
        |o4 ㄛ  12571  e4 ㄜ  12572  ai ㄞ  12574  ei ㄟ  12575  au ㄠ  12576  ou ㄡ  12577
        |an ㄢ  12578  en ㄣ  12579  aN ㄤ  12580  eN ㄥ  12581  er ㄦ  12582  i4 ㄧ  12583
        |u4 ㄨ  12584  iu ㄩ  12585  v4 ㄪ  12586  nG ㄫ  12587  gn ㄬ  12588  1c ㈠  12832
        |2c ㈡  12833  3c ㈢  12834  4c ㈣  12835  5c ㈤  12836  6c ㈥  12837  7c ㈦  12838
        |8c ㈧  12839  9c ㈨  12840  ff ﬀ  64256  fi ﬁ  64257  fl ﬂ  64258  ft ﬅ  64261
        |st ﬆ  64262  (0 ⓪  9450   (2 ②  9313   (1 ①  9312
      """.trimMargin()
    )
  }

  @Test
  fun `test digraph output with 32-bit custom digraphs`() {
    enterCommand("digraph cr 128308") // 🔴
    assertCommandOutput(
      "digraphs",
      """
        |NU ^@  10    SH ^A   1    SX ^B   2    EX ^C   3    ET ^D   4    EQ ^E   5
        |AK ^F   6    BL ^G   7    BS ^H   8    HT ^I   9    LF ^J  10    VT ^K  11
        |FF ^L  12    CR ^M  13    SO ^N  14    SI ^O  15    DL ^P  16    D1 ^Q  17
        |D2 ^R  18    D3 ^S  19    D4 ^T  20    NK ^U  21    SY ^V  22    EB ^W  23
        |CN ^X  24    EM ^Y  25    SB ^Z  26    EC ^[  27    FS ^\  28    GS ^]  29
        |RS ^^  30    US ^_  31    SP     32    Nb #   35    DO $   36    At @   64
        |<( [   91    // \   92    )> ]   93    '> ^   94    '! `   96    (! {  123
        |!! |  124    !) }  125    '? ~  126    DT ^? 127    PA <80> 128  HO <81> 129
        |BH <82> 130  NH <83> 131  IN <84> 132  NL <85> 133  SA <86> 134  ES <87> 135
        |HS <88> 136  HJ <89> 137  VS <8a> 138  PD <8b> 139  PU <8c> 140  RI <8d> 141
        |S2 <8e> 142  S3 <8f> 143  DC <90> 144  P1 <91> 145  P2 <92> 146  TS <93> 147
        |CC <94> 148  MW <95> 149  SG <96> 150  EG <97> 151  SS <98> 152  GC <99> 153
        |SC <9a> 154  CI <9b> 155  ST <9c> 156  OC <9d> 157  PM <9e> 158  AC <9f> 159
        |NS    160    !I ¡  161    ~! ¡  161    Ct ¢  162    c| ¢  162    Pd £  163
        |$$ £  163    Cu ¤  164    ox ¤  164    Ye ¥  165    Y- ¥  165    BB ¦  166
        ||| ¦  166    SE §  167    ': ¨  168    Co ©  169    cO ©  169    -a ª  170
        |<< «  171    NO ¬  172    -, ¬  172    -- <ad> 173  Rg ®  174    'm ¯  175
        |-= ¯  175    DG °  176    ~o °  176    +- ±  177    2S ²  178    22 ²  178
        |3S ³  179    33 ³  179    '' ´  180    My µ  181    PI ¶  182    pp ¶  182
        |.M ·  183    ~. ·  183    ', ¸  184    1S ¹  185    11 ¹  185    -o º  186
        |>> »  187    14 ¼  188    12 ½  189    34 ¾  190    ?I ¿  191    ~? ¿  191
        |A! À  192    A` À  192    A' Á  193    A> Â  194    A^ Â  194    A? Ã  195
        |A~ Ã  195    A: Ä  196    A" Ä  196    AA Å  197    A@ Å  197    AE Æ  198
        |C, Ç  199    E! È  200    E` È  200    E' É  201    E> Ê  202    E^ Ê  202
        |E: Ë  203    E" Ë  203    I! Ì  204    I` Ì  204    I' Í  205    I> Î  206
        |I^ Î  206    I: Ï  207    I" Ï  207    D- Ð  208    N? Ñ  209    N~ Ñ  209
        |O! Ò  210    O` Ò  210    O' Ó  211    O> Ô  212    O^ Ô  212    O? Õ  213
        |O~ Õ  213    O: Ö  214    *X ×  215    /\ ×  215    O/ Ø  216    U! Ù  217
        |U` Ù  217    U' Ú  218    U> Û  219    U^ Û  219    U: Ü  220    Y' Ý  221
        |TH Þ  222    Ip Þ  222    ss ß  223    a! à  224    a` à  224    a' á  225
        |a> â  226    a^ â  226    a? ã  227    a~ ã  227    a: ä  228    a" ä  228
        |aa å  229    a@ å  229    ae æ  230    c, ç  231    e! è  232    e` è  232
        |e' é  233    e> ê  234    e^ ê  234    e: ë  235    e" ë  235    i! ì  236
        |i` ì  236    i' í  237    i> î  238    i^ î  238    i: ï  239    d- ð  240
        |n? ñ  241    n~ ñ  241    o! ò  242    o` ò  242    o' ó  243    o> ô  244
        |o^ ô  244    o? õ  245    o~ õ  245    o: ö  246    -: ÷  247    o/ ø  248
        |u! ù  249    u` ù  249    u' ú  250    u> û  251    u^ û  251    u: ü  252
        |y' ý  253    th þ  254    y: ÿ  255    y" ÿ  255    A- Ā  256    a- ā  257
        |A( Ă  258    a( ă  259    A; Ą  260    a; ą  261    C' Ć  262    c' ć  263
        |C> Ĉ  264    c> ĉ  265    C. Ċ  266    c. ċ  267    C< Č  268    c< č  269
        |D< Ď  270    d< ď  271    D/ Đ  272    d/ đ  273    E- Ē  274    e- ē  275
        |E( Ĕ  276    e( ĕ  277    E. Ė  278    e. ė  279    E; Ę  280    e; ę  281
        |E< Ě  282    e< ě  283    G> Ĝ  284    g> ĝ  285    G( Ğ  286    g( ğ  287
        |G. Ġ  288    g. ġ  289    G, Ģ  290    g, ģ  291    H> Ĥ  292    h> ĥ  293
        |H/ Ħ  294    h/ ħ  295    I? Ĩ  296    i? ĩ  297    I- Ī  298    i- ī  299
        |I( Ĭ  300    i( ĭ  301    I; Į  302    i; į  303    I. İ  304    i. ı  305
        |IJ Ĳ  306    ij ĳ  307    J> Ĵ  308    j> ĵ  309    K, Ķ  310    k, ķ  311
        |kk ĸ  312    L' Ĺ  313    l' ĺ  314    L, Ļ  315    l, ļ  316    L< Ľ  317
        |l< ľ  318    L. Ŀ  319    l. ŀ  320    L/ Ł  321    l/ ł  322    N' Ń  323
        |n' ń  324    N, Ņ  325    n, ņ  326    N< Ň  327    n< ň  328    'n ŉ  329
        |NG Ŋ  330    ng ŋ  331    O- Ō  332    o- ō  333    O( Ŏ  334    o( ŏ  335
        |O" Ő  336    o" ő  337    OE Œ  338    oe œ  339    R' Ŕ  340    r' ŕ  341
        |R, Ŗ  342    r, ŗ  343    R< Ř  344    r< ř  345    S' Ś  346    s' ś  347
        |S> Ŝ  348    s> ŝ  349    S, Ş  350    s, ş  351    S< Š  352    s< š  353
        |T, Ţ  354    t, ţ  355    T< Ť  356    t< ť  357    T/ Ŧ  358    t/ ŧ  359
        |U? Ũ  360    u? ũ  361    U- Ū  362    u- ū  363    U( Ŭ  364    u( ŭ  365
        |U0 Ů  366    u0 ů  367    U" Ű  368    u" ű  369    U; Ų  370    u; ų  371
        |W> Ŵ  372    w> ŵ  373    Y> Ŷ  374    y> ŷ  375    Y: Ÿ  376    Z' Ź  377
        |z' ź  378    Z. Ż  379    z. ż  380    Z< Ž  381    z< ž  382    O9 Ơ  416
        |o9 ơ  417    OI Ƣ  418    oi ƣ  419    yr Ʀ  422    U9 Ư  431    u9 ư  432
        |Z/ Ƶ  437    z/ ƶ  438    ED Ʒ  439    A< Ǎ  461    a< ǎ  462    I< Ǐ  463
        |i< ǐ  464    O< Ǒ  465    o< ǒ  466    U< Ǔ  467    u< ǔ  468    A1 Ǟ  478
        |a1 ǟ  479    A7 Ǡ  480    a7 ǡ  481    A3 Ǣ  482    a3 ǣ  483    G/ Ǥ  484
        |g/ ǥ  485    G< Ǧ  486    g< ǧ  487    K< Ǩ  488    k< ǩ  489    O; Ǫ  490
        |o; ǫ  491    O1 Ǭ  492    o1 ǭ  493    EZ Ǯ  494    ez ǯ  495    j< ǰ  496
        |G' Ǵ  500    g' ǵ  501    ;S ʿ  703    '< ˇ  711    '( ˘  728    '. ˙  729
        |'0 ˚  730    '; ˛  731    '" ˝  733    A% Ά  902    E% Έ  904    Y% Ή  905
        |I% Ί  906    O% Ό  908    U% Ύ  910    W% Ώ  911    i3 ΐ  912    A* Α  913
        |B* Β  914    G* Γ  915    D* Δ  916    E* Ε  917    Z* Ζ  918    Y* Η  919
        |H* Θ  920    I* Ι  921    K* Κ  922    L* Λ  923    M* Μ  924    N* Ν  925
        |C* Ξ  926    O* Ο  927    P* Π  928    R* Ρ  929    S* Σ  931    T* Τ  932
        |U* Υ  933    F* Φ  934    X* Χ  935    Q* Ψ  936    W* Ω  937    J* Ϊ  938
        |V* Ϋ  939    a% ά  940    e% έ  941    y% ή  942    i% ί  943    u3 ΰ  944
        |a* α  945    b* β  946    g* γ  947    d* δ  948    e* ε  949    z* ζ  950
        |y* η  951    h* θ  952    i* ι  953    k* κ  954    l* λ  955    m* μ  956
        |n* ν  957    c* ξ  958    o* ο  959    p* π  960    r* ρ  961    *s ς  962
        |s* σ  963    t* τ  964    u* υ  965    f* φ  966    x* χ  967    q* ψ  968
        |w* ω  969    j* ϊ  970    v* ϋ  971    o% ό  972    u% ύ  973    w% ώ  974
        |'G Ϙ  984    ,G ϙ  985    T3 Ϛ  986    t3 ϛ  987    M3 Ϝ  988    m3 ϝ  989
        |K3 Ϟ  990    k3 ϟ  991    P3 Ϡ  992    p3 ϡ  993    '% ϴ  1012   j3 ϵ  1013
        |IO Ё  1025   D% Ђ  1026   G% Ѓ  1027   IE Є  1028   DS Ѕ  1029   II І  1030
        |YI Ї  1031   J% Ј  1032   LJ Љ  1033   NJ Њ  1034   Ts Ћ  1035   KJ Ќ  1036
        |V% Ў  1038   DZ Џ  1039   A= А  1040   B= Б  1041   V= В  1042   G= Г  1043
        |D= Д  1044   E= Е  1045   Z% Ж  1046   Z= З  1047   I= И  1048   J= Й  1049
        |K= К  1050   L= Л  1051   M= М  1052   N= Н  1053   O= О  1054   P= П  1055
        |R= Р  1056   S= С  1057   T= Т  1058   U= У  1059   F= Ф  1060   H= Х  1061
        |C= Ц  1062   C% Ч  1063   S% Ш  1064   Sc Щ  1065   =" Ъ  1066   Y= Ы  1067
        |%" Ь  1068   JE Э  1069   JU Ю  1070   JA Я  1071   a= а  1072   b= б  1073
        |v= в  1074   g= г  1075   d= д  1076   e= е  1077   z% ж  1078   z= з  1079
        |i= и  1080   j= й  1081   k= к  1082   l= л  1083   m= м  1084   n= н  1085
        |o= о  1086   p= п  1087   r= р  1088   s= с  1089   t= т  1090   u= у  1091
        |f= ф  1092   h= х  1093   c= ц  1094   c% ч  1095   s% ш  1096   sc щ  1097
        |=' ъ  1098   y= ы  1099   %' ь  1100   je э  1101   ju ю  1102   ja я  1103
        |io ё  1105   d% ђ  1106   g% ѓ  1107   ie є  1108   ds ѕ  1109   ii і  1110
        |yi ї  1111   j% ј  1112   lj љ  1113   nj њ  1114   ts ћ  1115   kj ќ  1116
        |v% ў  1118   dz џ  1119   Y3 Ѣ  1122   y3 ѣ  1123   O3 Ѫ  1130   o3 ѫ  1131
        |F3 Ѳ  1138   f3 ѳ  1139   V3 Ѵ  1140   v3 ѵ  1141   C3 Ҁ  1152   c3 ҁ  1153
        |G3 Ґ  1168   g3 ґ  1169   A+ ⁧א⁩  1488   B+ ⁧ב⁩  1489   G+ ⁧ג⁩  1490   D+ ⁧ד⁩  1491
        |H+ ⁧ה⁩  1492   W+ ⁧ו⁩  1493   Z+ ⁧ז⁩  1494   X+ ⁧ח⁩  1495   Tj ⁧ט⁩  1496   J+ ⁧י⁩  1497
        |K% ⁧ך⁩  1498   K+ ⁧כ⁩  1499   L+ ⁧ל⁩  1500   M% ⁧ם⁩  1501   M+ ⁧מ⁩  1502   N% ⁧ן⁩  1503
        |N+ ⁧נ⁩  1504   S+ ⁧ס⁩  1505   E+ ⁧ע⁩  1506   P% ⁧ף⁩  1507   P+ ⁧פ⁩  1508   Zj ⁧ץ⁩  1509
        |ZJ ⁧צ⁩  1510   Q+ ⁧ק⁩  1511   R+ ⁧ר⁩  1512   Sh ⁧ש⁩  1513   T+ ⁧ת⁩  1514   ,+ ،  1548
        |;+ ⁧؛⁩  1563   ?+ ⁧؟⁩  1567   H' ⁧ء⁩  1569   aM ⁧آ⁩  1570   aH ⁧أ⁩  1571   wH ⁧ؤ⁩  1572
        |ah ⁧إ⁩  1573   yH ⁧ئ⁩  1574   a+ ⁧ا⁩  1575   b+ ⁧ب⁩  1576   tm ⁧ة⁩  1577   t+ ⁧ت⁩  1578
        |tk ⁧ث⁩  1579   g+ ⁧ج⁩  1580   hk ⁧ح⁩  1581   x+ ⁧خ⁩  1582   d+ ⁧د⁩  1583   dk ⁧ذ⁩  1584
        |r+ ⁧ر⁩  1585   z+ ⁧ز⁩  1586   s+ ⁧س⁩  1587   sn ⁧ش⁩  1588   c+ ⁧ص⁩  1589   dd ⁧ض⁩  1590
        |tj ⁧ط⁩  1591   zH ⁧ظ⁩  1592   e+ ⁧ع⁩  1593   i+ ⁧غ⁩  1594   ++ ⁧ـ⁩  1600   f+ ⁧ف⁩  1601
        |q+ ⁧ق⁩  1602   k+ ⁧ك⁩  1603   l+ ⁧ل⁩  1604   m+ ⁧م⁩  1605   n+ ⁧ن⁩  1606   h+ ⁧ه⁩  1607
        |w+ ⁧و⁩  1608   j+ ⁧ى⁩  1609   y+ ⁧ي⁩  1610   :+  ً  1611   "+  ٌ  1612   =+  ٍ  1613
        |/+  َ  1614   '+  ُ  1615   1+  ِ  1616   3+  ّ  1617   0+  ْ  1618   aS  ٰ  1648
        |p+ ⁧پ⁩  1662   v+ ⁧ڤ⁩  1700   gf ⁧گ⁩  1711   0a ۰  1776   1a ۱  1777   2a ۲  1778
        |3a ۳  1779   4a ۴  1780   5a ۵  1781   6a ۶  1782   7a ۷  1783   8a ۸  1784
        |9a ۹  1785   B. Ḃ  7682   b. ḃ  7683   B_ Ḇ  7686   b_ ḇ  7687   D. Ḋ  7690
        |d. ḋ  7691   D_ Ḏ  7694   d_ ḏ  7695   D, Ḑ  7696   d, ḑ  7697   F. Ḟ  7710
        |f. ḟ  7711   G- Ḡ  7712   g- ḡ  7713   H. Ḣ  7714   h. ḣ  7715   H: Ḧ  7718
        |h: ḧ  7719   H, Ḩ  7720   h, ḩ  7721   K' Ḱ  7728   k' ḱ  7729   K_ Ḵ  7732
        |k_ ḵ  7733   L_ Ḻ  7738   l_ ḻ  7739   M' Ḿ  7742   m' ḿ  7743   M. Ṁ  7744
        |m. ṁ  7745   N. Ṅ  7748   n. ṅ  7749   N_ Ṉ  7752   n_ ṉ  7753   P' Ṕ  7764
        |p' ṕ  7765   P. Ṗ  7766   p. ṗ  7767   R. Ṙ  7768   r. ṙ  7769   R_ Ṟ  7774
        |r_ ṟ  7775   S. Ṡ  7776   s. ṡ  7777   T. Ṫ  7786   t. ṫ  7787   T_ Ṯ  7790
        |t_ ṯ  7791   V? Ṽ  7804   v? ṽ  7805   W! Ẁ  7808   W` Ẁ  7808   w! ẁ  7809
        |w` ẁ  7809   W' Ẃ  7810   w' ẃ  7811   W: Ẅ  7812   w: ẅ  7813   W. Ẇ  7814
        |w. ẇ  7815   X. Ẋ  7818   x. ẋ  7819   X: Ẍ  7820   x: ẍ  7821   Y. Ẏ  7822
        |y. ẏ  7823   Z> Ẑ  7824   z> ẑ  7825   Z_ Ẕ  7828   z_ ẕ  7829   h_ ẖ  7830
        |t: ẗ  7831   w0 ẘ  7832   y0 ẙ  7833   A2 Ả  7842   a2 ả  7843   E2 Ẻ  7866
        |e2 ẻ  7867   E? Ẽ  7868   e? ẽ  7869   I2 Ỉ  7880   i2 ỉ  7881   O2 Ỏ  7886
        |o2 ỏ  7887   U2 Ủ  7910   u2 ủ  7911   Y! Ỳ  7922   Y` Ỳ  7922   y! ỳ  7923
        |y` ỳ  7923   Y2 Ỷ  7926   y2 ỷ  7927   Y? Ỹ  7928   y? ỹ  7929   ;' ἀ  7936
        |,' ἁ  7937   ;! ἂ  7938   ,! ἃ  7939   ?; ἄ  7940   ?, ἅ  7941   !: ἆ  7942
        |?: ἇ  7943   1N    8194   1M    8195   3M    8196   4M    8197   6M    8198
        |1T    8201   1H    8202   -1 ‐  8208   -N –  8211   -M —  8212   -3 ―  8213
        |!2 ‖  8214   =2 ‗  8215   '6 ‘  8216   '9 ’  8217   .9 ‚  8218   9' ‛  8219
        |"6 “  8220   "9 ”  8221   :9 „  8222   9" ‟  8223   /- †  8224   /= ‡  8225
        |oo •  8226   .. ‥  8229   ,. …  8230   %0 ‰  8240   1' ′  8242   2' ″  8243
        |3' ‴  8244   4' ⁗  8279   1" ‵  8245   2" ‶  8246   3" ‷  8247   Ca ‸  8248
        |<1 ‹  8249   >1 ›  8250   :X ※  8251   '- ‾  8254   /f ⁄  8260   0S ⁰  8304
        |4S ⁴  8308   5S ⁵  8309   6S ⁶  8310   7S ⁷  8311   8S ⁸  8312   9S ⁹  8313
        |+S ⁺  8314   -S ⁻  8315   =S ⁼  8316   (S ⁽  8317   )S ⁾  8318   nS ⁿ  8319
        |0s ₀  8320   1s ₁  8321   2s ₂  8322   3s ₃  8323   4s ₄  8324   5s ₅  8325
        |6s ₆  8326   7s ₇  8327   8s ₈  8328   9s ₉  8329   +s ₊  8330   -s ₋  8331
        |=s ₌  8332   (s ₍  8333   )s ₎  8334   Li ₤  8356   Pt ₧  8359   W= ₩  8361
        |=e €  8364   Eu €  8364   =R ₽  8381   =P ₽  8381   oC ℃  8451   co ℅  8453
        |oF ℉  8457   N0 №  8470   PO ℗  8471   Rx ℞  8478   SM ℠  8480   TM ™  8482
        |Om Ω  8486   AO Å  8491   13 ⅓  8531   23 ⅔  8532   15 ⅕  8533   25 ⅖  8534
        |35 ⅗  8535   45 ⅘  8536   16 ⅙  8537   56 ⅚  8538   18 ⅛  8539   38 ⅜  8540
        |58 ⅝  8541   78 ⅞  8542   1R Ⅰ  8544   2R Ⅱ  8545   3R Ⅲ  8546   4R Ⅳ  8547
        |5R Ⅴ  8548   6R Ⅵ  8549   7R Ⅶ  8550   8R Ⅷ  8551   9R Ⅸ  8552   aR Ⅹ  8553
        |bR Ⅺ  8554   cR Ⅻ  8555   1r ⅰ  8560   2r ⅱ  8561   3r ⅲ  8562   4r ⅳ  8563
        |5r ⅴ  8564   6r ⅵ  8565   7r ⅶ  8566   8r ⅷ  8567   9r ⅸ  8568   ar ⅹ  8569
        |br ⅺ  8570   cr ⅻ  8571   <- ←  8592   -! ↑  8593   -> →  8594   -v ↓  8595
        |<> ↔  8596   UD ↕  8597   <= ⇐  8656   => ⇒  8658   == ⇔  8660   FA ∀  8704
        |dP ∂  8706   TE ∃  8707   /0 ∅  8709   DE ∆  8710   NB ∇  8711   (- ∈  8712
        |-) ∋  8715   *P ∏  8719   +Z ∑  8721   -2 −  8722   -+ ∓  8723   *- ∗  8727
        |Ob ∘  8728   Sb ∙  8729   RT √  8730   0( ∝  8733   00 ∞  8734   -L ∟  8735
        |-V ∠  8736   PP ∥  8741   AN ∧  8743   OR ∨  8744   (U ∩  8745   )U ∪  8746
        |In ∫  8747   DI ∬  8748   Io ∮  8750   .: ∴  8756   :. ∵  8757   :R ∶  8758
        |:: ∷  8759   ?1 ∼  8764   CG ∾  8766   ?- ≃  8771   ?= ≅  8773   ?2 ≈  8776
        |=? ≌  8780   HI ≓  8787   != ≠  8800   =3 ≡  8801   =< ≤  8804   >= ≥  8805
        |<* ≪  8810   *> ≫  8811   !< ≮  8814   !> ≯  8815   (C ⊂  8834   )C ⊃  8835
        |(_ ⊆  8838   )_ ⊇  8839   0. ⊙  8857   02 ⊚  8858   -T ⊥  8869   .P ⋅  8901
        |:3 ⋮  8942   .3 ⋯  8943   Eh ⌂  8962   <7 ⌈  8968   >7 ⌉  8969   7< ⌊  8970
        |7> ⌋  8971   NI ⌐  8976   (A ⌒  8978   TR ⌕  8981   Iu ⌠  8992   Il ⌡  8993
        |</ 〈  9001   /> 〉  9002   Vs ␣  9251   1h ⑀  9280   3h ⑁  9281   2h ⑂  9282
        |4h ⑃  9283   1j ⑆  9286   2j ⑇  9287   3j ⑈  9288   4j ⑉  9289   1. ⒈  9352
        |2. ⒉  9353   3. ⒊  9354   4. ⒋  9355   5. ⒌  9356   6. ⒍  9357   7. ⒎  9358
        |8. ⒏  9359   9. ⒐  9360   hh ─  9472   HH ━  9473   vv │  9474   VV ┃  9475
        |3- ┄  9476   3_ ┅  9477   3! ┆  9478   3/ ┇  9479   4- ┈  9480   4_ ┉  9481
        |4! ┊  9482   4/ ┋  9483   dr ┌  9484   dR ┍  9485   Dr ┎  9486   DR ┏  9487
        |dl ┐  9488   dL ┑  9489   Dl ┒  9490   LD ┓  9491   ur └  9492   uR ┕  9493
        |Ur ┖  9494   UR ┗  9495   ul ┘  9496   uL ┙  9497   Ul ┚  9498   UL ┛  9499
        |vr ├  9500   vR ┝  9501   Vr ┠  9504   VR ┣  9507   vl ┤  9508   vL ┥  9509
        |Vl ┨  9512   VL ┫  9515   dh ┬  9516   dH ┯  9519   Dh ┰  9520   DH ┳  9523
        |uh ┴  9524   uH ┷  9527   Uh ┸  9528   UH ┻  9531   vh ┼  9532   vH ┿  9535
        |Vh ╂  9538   VH ╋  9547   FD ╱  9585   BD ╲  9586   TB ▀  9600   LB ▄  9604
        |FB █  9608   lB ▌  9612   RB ▐  9616   .S ░  9617   :S ▒  9618   ?S ▓  9619
        |fS ■  9632   OS □  9633   RO ▢  9634   Rr ▣  9635   RF ▤  9636   RY ▥  9637
        |RH ▦  9638   RZ ▧  9639   RK ▨  9640   RX ▩  9641   sB ▪  9642   SR ▬  9644
        |Or ▭  9645   UT ▲  9650   uT △  9651   PR ▶  9654   Tr ▷  9655   Dt ▼  9660
        |dT ▽  9661   PL ◀  9664   Tl ◁  9665   Db ◆  9670   Dw ◇  9671   LZ ◊  9674
        |0m ○  9675   0o ◎  9678   0M ●  9679   0L ◐  9680   0R ◑  9681   Sn ◘  9688
        |Ic ◙  9689   Fd ◢  9698   Bd ◣  9699   *2 ★  9733   *1 ☆  9734   <H ☜  9756
        |>H ☞  9758   0u ☺  9786   0U ☻  9787   SU ☼  9788   Fm ♀  9792   Ml ♂  9794
        |cS ♠  9824   cH ♡  9825   cD ♢  9826   cC ♣  9827   Md ♩  9833   M8 ♪  9834
        |M2 ♫  9835   Mb ♭  9837   Mx ♮  9838   MX ♯  9839   OK ✓  10003  XX ✗  10007
        |-X ✠  10016  IS 　  12288  ,_ 、  12289  ._ 。  12290  +" 〃  12291  +_ 〄  12292
        |*_ 々  12293  ;_ 〆  12294  0_ 〇  12295  <+ 《  12298  >+ 》  12299  <' 「  12300
        |>' 」  12301  <" 『  12302  >" 』  12303  (" 【  12304  )" 】  12305  =T 〒  12306
        |=_ 〓  12307  (' 〔  12308  )' 〕  12309  (I 〖  12310  )I 〗  12311  -? 〜  12316
        |A5 ぁ  12353  a5 あ  12354  I5 ぃ  12355  i5 い  12356  U5 ぅ  12357  u5 う  12358
        |E5 ぇ  12359  e5 え  12360  O5 ぉ  12361  o5 お  12362  ka か  12363  ga が  12364
        |ki き  12365  gi ぎ  12366  ku く  12367  gu ぐ  12368  ke け  12369  ge げ  12370
        |ko こ  12371  go ご  12372  sa さ  12373  za ざ  12374  si し  12375  zi じ  12376
        |su す  12377  zu ず  12378  se せ  12379  ze ぜ  12380  so そ  12381  zo ぞ  12382
        |ta た  12383  da だ  12384  ti ち  12385  di ぢ  12386  tU っ  12387  tu つ  12388
        |du づ  12389  te て  12390  de で  12391  to と  12392  do ど  12393  na な  12394
        |ni に  12395  nu ぬ  12396  ne ね  12397  no の  12398  ha は  12399  ba ば  12400
        |pa ぱ  12401  hi ひ  12402  bi び  12403  pi ぴ  12404  hu ふ  12405  bu ぶ  12406
        |pu ぷ  12407  he へ  12408  be べ  12409  pe ぺ  12410  ho ほ  12411  bo ぼ  12412
        |po ぽ  12413  ma ま  12414  mi み  12415  mu む  12416  me め  12417  mo も  12418
        |yA ゃ  12419  ya や  12420  yU ゅ  12421  yu ゆ  12422  yO ょ  12423  yo よ  12424
        |ra ら  12425  ri り  12426  ru る  12427  re れ  12428  ro ろ  12429  wA ゎ  12430
        |wa わ  12431  wi ゐ  12432  we ゑ  12433  wo を  12434  n5 ん  12435  vu ゔ  12436
        |"5 ゛  12443  05 ゜  12444  *5 ゝ  12445  +5 ゞ  12446  a6 ァ  12449  A6 ア  12450
        |i6 ィ  12451  I6 イ  12452  u6 ゥ  12453  U6 ウ  12454  e6 ェ  12455  E6 エ  12456
        |o6 ォ  12457  O6 オ  12458  Ka カ  12459  Ga ガ  12460  Ki キ  12461  Gi ギ  12462
        |Ku ク  12463  Gu グ  12464  Ke ケ  12465  Ge ゲ  12466  Ko コ  12467  Go ゴ  12468
        |Sa サ  12469  Za ザ  12470  Si シ  12471  Zi ジ  12472  Su ス  12473  Zu ズ  12474
        |Se セ  12475  Ze ゼ  12476  So ソ  12477  Zo ゾ  12478  Ta タ  12479  Da ダ  12480
        |Ti チ  12481  Di ヂ  12482  TU ッ  12483  Tu ツ  12484  Du ヅ  12485  Te テ  12486
        |De デ  12487  To ト  12488  Do ド  12489  Na ナ  12490  Ni ニ  12491  Nu ヌ  12492
        |Ne ネ  12493  No ノ  12494  Ha ハ  12495  Ba バ  12496  Pa パ  12497  Hi ヒ  12498
        |Bi ビ  12499  Pi ピ  12500  Hu フ  12501  Bu ブ  12502  Pu プ  12503  He ヘ  12504
        |Be ベ  12505  Pe ペ  12506  Ho ホ  12507  Bo ボ  12508  Po ポ  12509  Ma マ  12510
        |Mi ミ  12511  Mu ム  12512  Me メ  12513  Mo モ  12514  YA ャ  12515  Ya ヤ  12516
        |YU ュ  12517  Yu ユ  12518  YO ョ  12519  Yo ヨ  12520  Ra ラ  12521  Ri リ  12522
        |Ru ル  12523  Re レ  12524  Ro ロ  12525  WA ヮ  12526  Wa ワ  12527  Wi ヰ  12528
        |We ヱ  12529  Wo ヲ  12530  N6 ン  12531  Vu ヴ  12532  KA ヵ  12533  KE ヶ  12534
        |Va ヷ  12535  Vi ヸ  12536  Ve ヹ  12537  Vo ヺ  12538  .6 ・  12539  -6 ー  12540
        |*6 ヽ  12541  +6 ヾ  12542  b4 ㄅ  12549  p4 ㄆ  12550  m4 ㄇ  12551  f4 ㄈ  12552
        |d4 ㄉ  12553  t4 ㄊ  12554  n4 ㄋ  12555  l4 ㄌ  12556  g4 ㄍ  12557  k4 ㄎ  12558
        |h4 ㄏ  12559  j4 ㄐ  12560  q4 ㄑ  12561  x4 ㄒ  12562  zh ㄓ  12563  ch ㄔ  12564
        |sh ㄕ  12565  r4 ㄖ  12566  z4 ㄗ  12567  c4 ㄘ  12568  s4 ㄙ  12569  a4 ㄚ  12570
        |o4 ㄛ  12571  e4 ㄜ  12572  ai ㄞ  12574  ei ㄟ  12575  au ㄠ  12576  ou ㄡ  12577
        |an ㄢ  12578  en ㄣ  12579  aN ㄤ  12580  eN ㄥ  12581  er ㄦ  12582  i4 ㄧ  12583
        |u4 ㄨ  12584  iu ㄩ  12585  v4 ㄪ  12586  nG ㄫ  12587  gn ㄬ  12588  1c ㈠  12832
        |2c ㈡  12833  3c ㈢  12834  4c ㈣  12835  5c ㈤  12836  6c ㈥  12837  7c ㈦  12838
        |8c ㈧  12839  9c ㈨  12840  ff ﬀ  64256  fi ﬁ  64257  fl ﬂ  64258  ft ﬅ  64261
        |st ﬆ  64262  cr 🔴 128308
      """.trimMargin()
    )
  }

  @Test
  fun `test digraph output with headers and custom digraphs`() {
    enterCommand("digraphs (0 9450 (2 9313 (1 9312")
    assertCommandOutput(
      "digraphs!",
      """
        |NU ^@  10    SH ^A   1    SX ^B   2    EX ^C   3    ET ^D   4    EQ ^E   5
        |AK ^F   6    BL ^G   7    BS ^H   8    HT ^I   9    LF ^J  10    VT ^K  11
        |FF ^L  12    CR ^M  13    SO ^N  14    SI ^O  15    DL ^P  16    D1 ^Q  17
        |D2 ^R  18    D3 ^S  19    D4 ^T  20    NK ^U  21    SY ^V  22    EB ^W  23
        |CN ^X  24    EM ^Y  25    SB ^Z  26    EC ^[  27    FS ^\  28    GS ^]  29
        |RS ^^  30    US ^_  31    SP     32    Nb #   35    DO $   36    At @   64
        |<( [   91    // \   92    )> ]   93    '> ^   94    '! `   96    (! {  123
        |!! |  124    !) }  125    '? ~  126    DT ^? 127    PA <80> 128  HO <81> 129
        |BH <82> 130  NH <83> 131  IN <84> 132  NL <85> 133  SA <86> 134  ES <87> 135
        |HS <88> 136  HJ <89> 137  VS <8a> 138  PD <8b> 139  PU <8c> 140  RI <8d> 141
        |S2 <8e> 142  S3 <8f> 143  DC <90> 144  P1 <91> 145  P2 <92> 146  TS <93> 147
        |CC <94> 148  MW <95> 149  SG <96> 150  EG <97> 151  SS <98> 152  GC <99> 153
        |SC <9a> 154  CI <9b> 155  ST <9c> 156  OC <9d> 157  PM <9e> 158  AC <9f> 159
        |NS    160
        |Latin supplement
        |!I ¡  161    ~! ¡  161    Ct ¢  162    c| ¢  162    Pd £  163    $$ £  163
        |Cu ¤  164    ox ¤  164    Ye ¥  165    Y- ¥  165    BB ¦  166    || ¦  166
        |SE §  167    ': ¨  168    Co ©  169    cO ©  169    -a ª  170    << «  171
        |NO ¬  172    -, ¬  172    -- <ad> 173  Rg ®  174    'm ¯  175    -= ¯  175
        |DG °  176    ~o °  176    +- ±  177    2S ²  178    22 ²  178    3S ³  179
        |33 ³  179    '' ´  180    My µ  181    PI ¶  182    pp ¶  182    .M ·  183
        |~. ·  183    ', ¸  184    1S ¹  185    11 ¹  185    -o º  186    >> »  187
        |14 ¼  188    12 ½  189    34 ¾  190    ?I ¿  191    ~? ¿  191    A! À  192
        |A` À  192    A' Á  193    A> Â  194    A^ Â  194    A? Ã  195    A~ Ã  195
        |A: Ä  196    A" Ä  196    AA Å  197    A@ Å  197    AE Æ  198    C, Ç  199
        |E! È  200    E` È  200    E' É  201    E> Ê  202    E^ Ê  202    E: Ë  203
        |E" Ë  203    I! Ì  204    I` Ì  204    I' Í  205    I> Î  206    I^ Î  206
        |I: Ï  207    I" Ï  207    D- Ð  208    N? Ñ  209    N~ Ñ  209    O! Ò  210
        |O` Ò  210    O' Ó  211    O> Ô  212    O^ Ô  212    O? Õ  213    O~ Õ  213
        |O: Ö  214    *X ×  215    /\ ×  215    O/ Ø  216    U! Ù  217    U` Ù  217
        |U' Ú  218    U> Û  219    U^ Û  219    U: Ü  220    Y' Ý  221    TH Þ  222
        |Ip Þ  222    ss ß  223    a! à  224    a` à  224    a' á  225    a> â  226
        |a^ â  226    a? ã  227    a~ ã  227    a: ä  228    a" ä  228    aa å  229
        |a@ å  229    ae æ  230    c, ç  231    e! è  232    e` è  232    e' é  233
        |e> ê  234    e^ ê  234    e: ë  235    e" ë  235    i! ì  236    i` ì  236
        |i' í  237    i> î  238    i^ î  238    i: ï  239    d- ð  240    n? ñ  241
        |n~ ñ  241    o! ò  242    o` ò  242    o' ó  243    o> ô  244    o^ ô  244
        |o? õ  245    o~ õ  245    o: ö  246    -: ÷  247    o/ ø  248    u! ù  249
        |u` ù  249    u' ú  250    u> û  251    u^ û  251    u: ü  252    y' ý  253
        |th þ  254    y: ÿ  255    y" ÿ  255    A- Ā  256    a- ā  257    A( Ă  258
        |a( ă  259    A; Ą  260    a; ą  261    C' Ć  262    c' ć  263    C> Ĉ  264
        |c> ĉ  265    C. Ċ  266    c. ċ  267    C< Č  268    c< č  269    D< Ď  270
        |d< ď  271    D/ Đ  272    d/ đ  273    E- Ē  274    e- ē  275    E( Ĕ  276
        |e( ĕ  277    E. Ė  278    e. ė  279    E; Ę  280    e; ę  281    E< Ě  282
        |e< ě  283    G> Ĝ  284    g> ĝ  285    G( Ğ  286    g( ğ  287    G. Ġ  288
        |g. ġ  289    G, Ģ  290    g, ģ  291    H> Ĥ  292    h> ĥ  293    H/ Ħ  294
        |h/ ħ  295    I? Ĩ  296    i? ĩ  297    I- Ī  298    i- ī  299    I( Ĭ  300
        |i( ĭ  301    I; Į  302    i; į  303    I. İ  304    i. ı  305    IJ Ĳ  306
        |ij ĳ  307    J> Ĵ  308    j> ĵ  309    K, Ķ  310    k, ķ  311    kk ĸ  312
        |L' Ĺ  313    l' ĺ  314    L, Ļ  315    l, ļ  316    L< Ľ  317    l< ľ  318
        |L. Ŀ  319    l. ŀ  320    L/ Ł  321    l/ ł  322    N' Ń  323    n' ń  324
        |N, Ņ  325    n, ņ  326    N< Ň  327    n< ň  328    'n ŉ  329    NG Ŋ  330
        |ng ŋ  331    O- Ō  332    o- ō  333    O( Ŏ  334    o( ŏ  335    O" Ő  336
        |o" ő  337    OE Œ  338    oe œ  339    R' Ŕ  340    r' ŕ  341    R, Ŗ  342
        |r, ŗ  343    R< Ř  344    r< ř  345    S' Ś  346    s' ś  347    S> Ŝ  348
        |s> ŝ  349    S, Ş  350    s, ş  351    S< Š  352    s< š  353    T, Ţ  354
        |t, ţ  355    T< Ť  356    t< ť  357    T/ Ŧ  358    t/ ŧ  359    U? Ũ  360
        |u? ũ  361    U- Ū  362    u- ū  363    U( Ŭ  364    u( ŭ  365    U0 Ů  366
        |u0 ů  367    U" Ű  368    u" ű  369    U; Ų  370    u; ų  371    W> Ŵ  372
        |w> ŵ  373    Y> Ŷ  374    y> ŷ  375    Y: Ÿ  376    Z' Ź  377    z' ź  378
        |Z. Ż  379    z. ż  380    Z< Ž  381    z< ž  382    O9 Ơ  416    o9 ơ  417
        |OI Ƣ  418    oi ƣ  419    yr Ʀ  422    U9 Ư  431    u9 ư  432    Z/ Ƶ  437
        |z/ ƶ  438    ED Ʒ  439    A< Ǎ  461    a< ǎ  462    I< Ǐ  463    i< ǐ  464
        |O< Ǒ  465    o< ǒ  466    U< Ǔ  467    u< ǔ  468    A1 Ǟ  478    a1 ǟ  479
        |A7 Ǡ  480    a7 ǡ  481    A3 Ǣ  482    a3 ǣ  483    G/ Ǥ  484    g/ ǥ  485
        |G< Ǧ  486    g< ǧ  487    K< Ǩ  488    k< ǩ  489    O; Ǫ  490    o; ǫ  491
        |O1 Ǭ  492    o1 ǭ  493    EZ Ǯ  494    ez ǯ  495    j< ǰ  496    G' Ǵ  500
        |g' ǵ  501    ;S ʿ  703    '< ˇ  711    '( ˘  728    '. ˙  729    '0 ˚  730
        |'; ˛  731    '" ˝  733
        |Greek and Coptic
        |A% Ά  902    E% Έ  904    Y% Ή  905    I% Ί  906    O% Ό  908    U% Ύ  910
        |W% Ώ  911    i3 ΐ  912    A* Α  913    B* Β  914    G* Γ  915    D* Δ  916
        |E* Ε  917    Z* Ζ  918    Y* Η  919    H* Θ  920    I* Ι  921    K* Κ  922
        |L* Λ  923    M* Μ  924    N* Ν  925    C* Ξ  926    O* Ο  927    P* Π  928
        |R* Ρ  929    S* Σ  931    T* Τ  932    U* Υ  933    F* Φ  934    X* Χ  935
        |Q* Ψ  936    W* Ω  937    J* Ϊ  938    V* Ϋ  939    a% ά  940    e% έ  941
        |y% ή  942    i% ί  943    u3 ΰ  944    a* α  945    b* β  946    g* γ  947
        |d* δ  948    e* ε  949    z* ζ  950    y* η  951    h* θ  952    i* ι  953
        |k* κ  954    l* λ  955    m* μ  956    n* ν  957    c* ξ  958    o* ο  959
        |p* π  960    r* ρ  961    *s ς  962    s* σ  963    t* τ  964    u* υ  965
        |f* φ  966    x* χ  967    q* ψ  968    w* ω  969    j* ϊ  970    v* ϋ  971
        |o% ό  972    u% ύ  973    w% ώ  974    'G Ϙ  984    ,G ϙ  985    T3 Ϛ  986
        |t3 ϛ  987    M3 Ϝ  988    m3 ϝ  989    K3 Ϟ  990    k3 ϟ  991    P3 Ϡ  992
        |p3 ϡ  993    '% ϴ  1012   j3 ϵ  1013
        |Cyrillic
        |IO Ё  1025   D% Ђ  1026   G% Ѓ  1027   IE Є  1028   DS Ѕ  1029   II І  1030
        |YI Ї  1031   J% Ј  1032   LJ Љ  1033   NJ Њ  1034   Ts Ћ  1035   KJ Ќ  1036
        |V% Ў  1038   DZ Џ  1039   A= А  1040   B= Б  1041   V= В  1042   G= Г  1043
        |D= Д  1044   E= Е  1045   Z% Ж  1046   Z= З  1047   I= И  1048   J= Й  1049
        |K= К  1050   L= Л  1051   M= М  1052   N= Н  1053   O= О  1054   P= П  1055
        |R= Р  1056   S= С  1057   T= Т  1058   U= У  1059   F= Ф  1060   H= Х  1061
        |C= Ц  1062   C% Ч  1063   S% Ш  1064   Sc Щ  1065   =" Ъ  1066   Y= Ы  1067
        |%" Ь  1068   JE Э  1069   JU Ю  1070   JA Я  1071   a= а  1072   b= б  1073
        |v= в  1074   g= г  1075   d= д  1076   e= е  1077   z% ж  1078   z= з  1079
        |i= и  1080   j= й  1081   k= к  1082   l= л  1083   m= м  1084   n= н  1085
        |o= о  1086   p= п  1087   r= р  1088   s= с  1089   t= т  1090   u= у  1091
        |f= ф  1092   h= х  1093   c= ц  1094   c% ч  1095   s% ш  1096   sc щ  1097
        |=' ъ  1098   y= ы  1099   %' ь  1100   je э  1101   ju ю  1102   ja я  1103
        |io ё  1105   d% ђ  1106   g% ѓ  1107   ie є  1108   ds ѕ  1109   ii і  1110
        |yi ї  1111   j% ј  1112   lj љ  1113   nj њ  1114   ts ћ  1115   kj ќ  1116
        |v% ў  1118   dz џ  1119   Y3 Ѣ  1122   y3 ѣ  1123   O3 Ѫ  1130   o3 ѫ  1131
        |F3 Ѳ  1138   f3 ѳ  1139   V3 Ѵ  1140   v3 ѵ  1141   C3 Ҁ  1152   c3 ҁ  1153
        |G3 Ґ  1168   g3 ґ  1169
        |Hebrew
        |A+ ⁧א⁩  1488   B+ ⁧ב⁩  1489   G+ ⁧ג⁩  1490   D+ ⁧ד⁩  1491   H+ ⁧ה⁩  1492   W+ ⁧ו⁩  1493
        |Z+ ⁧ז⁩  1494   X+ ⁧ח⁩  1495   Tj ⁧ט⁩  1496   J+ ⁧י⁩  1497   K% ⁧ך⁩  1498   K+ ⁧כ⁩  1499
        |L+ ⁧ל⁩  1500   M% ⁧ם⁩  1501   M+ ⁧מ⁩  1502   N% ⁧ן⁩  1503   N+ ⁧נ⁩  1504   S+ ⁧ס⁩  1505
        |E+ ⁧ע⁩  1506   P% ⁧ף⁩  1507   P+ ⁧פ⁩  1508   Zj ⁧ץ⁩  1509   ZJ ⁧צ⁩  1510   Q+ ⁧ק⁩  1511
        |R+ ⁧ר⁩  1512   Sh ⁧ש⁩  1513   T+ ⁧ת⁩  1514
        |Arabic
        |,+ ،  1548   ;+ ⁧؛⁩  1563   ?+ ⁧؟⁩  1567   H' ⁧ء⁩  1569   aM ⁧آ⁩  1570   aH ⁧أ⁩  1571
        |wH ⁧ؤ⁩  1572   ah ⁧إ⁩  1573   yH ⁧ئ⁩  1574   a+ ⁧ا⁩  1575   b+ ⁧ب⁩  1576   tm ⁧ة⁩  1577
        |t+ ⁧ت⁩  1578   tk ⁧ث⁩  1579   g+ ⁧ج⁩  1580   hk ⁧ح⁩  1581   x+ ⁧خ⁩  1582   d+ ⁧د⁩  1583
        |dk ⁧ذ⁩  1584   r+ ⁧ر⁩  1585   z+ ⁧ز⁩  1586   s+ ⁧س⁩  1587   sn ⁧ش⁩  1588   c+ ⁧ص⁩  1589
        |dd ⁧ض⁩  1590   tj ⁧ط⁩  1591   zH ⁧ظ⁩  1592   e+ ⁧ع⁩  1593   i+ ⁧غ⁩  1594   ++ ⁧ـ⁩  1600
        |f+ ⁧ف⁩  1601   q+ ⁧ق⁩  1602   k+ ⁧ك⁩  1603   l+ ⁧ل⁩  1604   m+ ⁧م⁩  1605   n+ ⁧ن⁩  1606
        |h+ ⁧ه⁩  1607   w+ ⁧و⁩  1608   j+ ⁧ى⁩  1609   y+ ⁧ي⁩  1610   :+  ً  1611   "+  ٌ  1612
        |=+  ٍ  1613   /+  َ  1614   '+  ُ  1615   1+  ِ  1616   3+  ّ  1617   0+  ْ  1618
        |aS  ٰ  1648   p+ ⁧پ⁩  1662   v+ ⁧ڤ⁩  1700   gf ⁧گ⁩  1711   0a ۰  1776   1a ۱  1777
        |2a ۲  1778   3a ۳  1779   4a ۴  1780   5a ۵  1781   6a ۶  1782   7a ۷  1783
        |8a ۸  1784   9a ۹  1785
        |Latin extended
        |B. Ḃ  7682   b. ḃ  7683   B_ Ḇ  7686   b_ ḇ  7687   D. Ḋ  7690   d. ḋ  7691
        |D_ Ḏ  7694   d_ ḏ  7695   D, Ḑ  7696   d, ḑ  7697   F. Ḟ  7710   f. ḟ  7711
        |G- Ḡ  7712   g- ḡ  7713   H. Ḣ  7714   h. ḣ  7715   H: Ḧ  7718   h: ḧ  7719
        |H, Ḩ  7720   h, ḩ  7721   K' Ḱ  7728   k' ḱ  7729   K_ Ḵ  7732   k_ ḵ  7733
        |L_ Ḻ  7738   l_ ḻ  7739   M' Ḿ  7742   m' ḿ  7743   M. Ṁ  7744   m. ṁ  7745
        |N. Ṅ  7748   n. ṅ  7749   N_ Ṉ  7752   n_ ṉ  7753   P' Ṕ  7764   p' ṕ  7765
        |P. Ṗ  7766   p. ṗ  7767   R. Ṙ  7768   r. ṙ  7769   R_ Ṟ  7774   r_ ṟ  7775
        |S. Ṡ  7776   s. ṡ  7777   T. Ṫ  7786   t. ṫ  7787   T_ Ṯ  7790   t_ ṯ  7791
        |V? Ṽ  7804   v? ṽ  7805   W! Ẁ  7808   W` Ẁ  7808   w! ẁ  7809   w` ẁ  7809
        |W' Ẃ  7810   w' ẃ  7811   W: Ẅ  7812   w: ẅ  7813   W. Ẇ  7814   w. ẇ  7815
        |X. Ẋ  7818   x. ẋ  7819   X: Ẍ  7820   x: ẍ  7821   Y. Ẏ  7822   y. ẏ  7823
        |Z> Ẑ  7824   z> ẑ  7825   Z_ Ẕ  7828   z_ ẕ  7829   h_ ẖ  7830   t: ẗ  7831
        |w0 ẘ  7832   y0 ẙ  7833   A2 Ả  7842   a2 ả  7843   E2 Ẻ  7866   e2 ẻ  7867
        |E? Ẽ  7868   e? ẽ  7869   I2 Ỉ  7880   i2 ỉ  7881   O2 Ỏ  7886   o2 ỏ  7887
        |U2 Ủ  7910   u2 ủ  7911   Y! Ỳ  7922   Y` Ỳ  7922   y! ỳ  7923   y` ỳ  7923
        |Y2 Ỷ  7926   y2 ỷ  7927   Y? Ỹ  7928   y? ỹ  7929
        |Greek extended
        |;' ἀ  7936   ,' ἁ  7937   ;! ἂ  7938   ,! ἃ  7939   ?; ἄ  7940   ?, ἅ  7941
        |!: ἆ  7942   ?: ἇ  7943
        |Punctuation
        |1N    8194   1M    8195   3M    8196   4M    8197   6M    8198   1T    8201
        |1H    8202   -1 ‐  8208   -N –  8211   -M —  8212   -3 ―  8213   !2 ‖  8214
        |=2 ‗  8215   '6 ‘  8216   '9 ’  8217   .9 ‚  8218   9' ‛  8219   "6 “  8220
        |"9 ”  8221   :9 „  8222   9" ‟  8223   /- †  8224   /= ‡  8225   oo •  8226
        |.. ‥  8229   ,. …  8230   %0 ‰  8240   1' ′  8242   2' ″  8243   3' ‴  8244
        |4' ⁗  8279   1" ‵  8245   2" ‶  8246   3" ‷  8247   Ca ‸  8248   <1 ‹  8249
        |>1 ›  8250   :X ※  8251   '- ‾  8254   /f ⁄  8260
        |Super- and subscripts
        |0S ⁰  8304   4S ⁴  8308   5S ⁵  8309   6S ⁶  8310   7S ⁷  8311   8S ⁸  8312
        |9S ⁹  8313   +S ⁺  8314   -S ⁻  8315   =S ⁼  8316   (S ⁽  8317   )S ⁾  8318
        |nS ⁿ  8319   0s ₀  8320   1s ₁  8321   2s ₂  8322   3s ₃  8323   4s ₄  8324
        |5s ₅  8325   6s ₆  8326   7s ₇  8327   8s ₈  8328   9s ₉  8329   +s ₊  8330
        |-s ₋  8331   =s ₌  8332   (s ₍  8333   )s ₎  8334
        |Currency
        |Li ₤  8356   Pt ₧  8359   W= ₩  8361   =e €  8364   Eu €  8364   =R ₽  8381
        |=P ₽  8381
        |Other
        |oC ℃  8451   co ℅  8453   oF ℉  8457   N0 №  8470   PO ℗  8471   Rx ℞  8478
        |SM ℠  8480   TM ™  8482   Om Ω  8486   AO Å  8491   13 ⅓  8531   23 ⅔  8532
        |15 ⅕  8533   25 ⅖  8534   35 ⅗  8535   45 ⅘  8536   16 ⅙  8537   56 ⅚  8538
        |18 ⅛  8539   38 ⅜  8540   58 ⅝  8541   78 ⅞  8542
        |Roman numbers
        |1R Ⅰ  8544   2R Ⅱ  8545   3R Ⅲ  8546   4R Ⅳ  8547   5R Ⅴ  8548   6R Ⅵ  8549
        |7R Ⅶ  8550   8R Ⅷ  8551   9R Ⅸ  8552   aR Ⅹ  8553   bR Ⅺ  8554   cR Ⅻ  8555
        |1r ⅰ  8560   2r ⅱ  8561   3r ⅲ  8562   4r ⅳ  8563   5r ⅴ  8564   6r ⅵ  8565
        |7r ⅶ  8566   8r ⅷ  8567   9r ⅸ  8568   ar ⅹ  8569   br ⅺ  8570   cr ⅻ  8571
        |Arrows
        |<- ←  8592   -! ↑  8593   -> →  8594   -v ↓  8595   <> ↔  8596   UD ↕  8597
        |<= ⇐  8656   => ⇒  8658   == ⇔  8660
        |Mathematical operators
        |FA ∀  8704   dP ∂  8706   TE ∃  8707   /0 ∅  8709   DE ∆  8710   NB ∇  8711
        |(- ∈  8712   -) ∋  8715   *P ∏  8719   +Z ∑  8721   -2 −  8722   -+ ∓  8723
        |*- ∗  8727   Ob ∘  8728   Sb ∙  8729   RT √  8730   0( ∝  8733   00 ∞  8734
        |-L ∟  8735   -V ∠  8736   PP ∥  8741   AN ∧  8743   OR ∨  8744   (U ∩  8745
        |)U ∪  8746   In ∫  8747   DI ∬  8748   Io ∮  8750   .: ∴  8756   :. ∵  8757
        |:R ∶  8758   :: ∷  8759   ?1 ∼  8764   CG ∾  8766   ?- ≃  8771   ?= ≅  8773
        |?2 ≈  8776   =? ≌  8780   HI ≓  8787   != ≠  8800   =3 ≡  8801   =< ≤  8804
        |>= ≥  8805   <* ≪  8810   *> ≫  8811   !< ≮  8814   !> ≯  8815   (C ⊂  8834
        |)C ⊃  8835   (_ ⊆  8838   )_ ⊇  8839   0. ⊙  8857   02 ⊚  8858   -T ⊥  8869
        |.P ⋅  8901   :3 ⋮  8942   .3 ⋯  8943
        |Technical
        |Eh ⌂  8962   <7 ⌈  8968   >7 ⌉  8969   7< ⌊  8970   7> ⌋  8971   NI ⌐  8976
        |(A ⌒  8978   TR ⌕  8981   Iu ⌠  8992   Il ⌡  8993   </ 〈  9001   /> 〉  9002
        |Other
        |Vs ␣  9251   1h ⑀  9280   3h ⑁  9281   2h ⑂  9282   4h ⑃  9283   1j ⑆  9286
        |2j ⑇  9287   3j ⑈  9288   4j ⑉  9289   1. ⒈  9352   2. ⒉  9353   3. ⒊  9354
        |4. ⒋  9355   5. ⒌  9356   6. ⒍  9357   7. ⒎  9358   8. ⒏  9359   9. ⒐  9360
        |Box drawing
        |hh ─  9472   HH ━  9473   vv │  9474   VV ┃  9475   3- ┄  9476   3_ ┅  9477
        |3! ┆  9478   3/ ┇  9479   4- ┈  9480   4_ ┉  9481   4! ┊  9482   4/ ┋  9483
        |dr ┌  9484   dR ┍  9485   Dr ┎  9486   DR ┏  9487   dl ┐  9488   dL ┑  9489
        |Dl ┒  9490   LD ┓  9491   ur └  9492   uR ┕  9493   Ur ┖  9494   UR ┗  9495
        |ul ┘  9496   uL ┙  9497   Ul ┚  9498   UL ┛  9499   vr ├  9500   vR ┝  9501
        |Vr ┠  9504   VR ┣  9507   vl ┤  9508   vL ┥  9509   Vl ┨  9512   VL ┫  9515
        |dh ┬  9516   dH ┯  9519   Dh ┰  9520   DH ┳  9523   uh ┴  9524   uH ┷  9527
        |Uh ┸  9528   UH ┻  9531   vh ┼  9532   vH ┿  9535   Vh ╂  9538   VH ╋  9547
        |FD ╱  9585   BD ╲  9586
        |Block elements
        |TB ▀  9600   LB ▄  9604   FB █  9608   lB ▌  9612   RB ▐  9616   .S ░  9617
        |:S ▒  9618   ?S ▓  9619
        |Geometric shapes
        |fS ■  9632   OS □  9633   RO ▢  9634   Rr ▣  9635   RF ▤  9636   RY ▥  9637
        |RH ▦  9638   RZ ▧  9639   RK ▨  9640   RX ▩  9641   sB ▪  9642   SR ▬  9644
        |Or ▭  9645   UT ▲  9650   uT △  9651   PR ▶  9654   Tr ▷  9655   Dt ▼  9660
        |dT ▽  9661   PL ◀  9664   Tl ◁  9665   Db ◆  9670   Dw ◇  9671   LZ ◊  9674
        |0m ○  9675   0o ◎  9678   0M ●  9679   0L ◐  9680   0R ◑  9681   Sn ◘  9688
        |Ic ◙  9689   Fd ◢  9698   Bd ◣  9699
        |Symbols
        |*2 ★  9733   *1 ☆  9734   <H ☜  9756   >H ☞  9758   0u ☺  9786   0U ☻  9787
        |SU ☼  9788   Fm ♀  9792   Ml ♂  9794   cS ♠  9824   cH ♡  9825   cD ♢  9826
        |cC ♣  9827   Md ♩  9833   M8 ♪  9834   M2 ♫  9835   Mb ♭  9837   Mx ♮  9838
        |MX ♯  9839
        |Dingbats
        |OK ✓  10003  XX ✗  10007  -X ✠  10016
        |CJK symbols and punctuation
        |IS 　  12288  ,_ 、  12289  ._ 。  12290  +" 〃  12291  +_ 〄  12292  *_ 々  12293
        |;_ 〆  12294  0_ 〇  12295  <+ 《  12298  >+ 》  12299  <' 「  12300  >' 」  12301
        |<" 『  12302  >" 』  12303  (" 【  12304  )" 】  12305  =T 〒  12306  =_ 〓  12307
        |(' 〔  12308  )' 〕  12309  (I 〖  12310  )I 〗  12311  -? 〜  12316
        |Hiragana
        |A5 ぁ  12353  a5 あ  12354  I5 ぃ  12355  i5 い  12356  U5 ぅ  12357  u5 う  12358
        |E5 ぇ  12359  e5 え  12360  O5 ぉ  12361  o5 お  12362  ka か  12363  ga が  12364
        |ki き  12365  gi ぎ  12366  ku く  12367  gu ぐ  12368  ke け  12369  ge げ  12370
        |ko こ  12371  go ご  12372  sa さ  12373  za ざ  12374  si し  12375  zi じ  12376
        |su す  12377  zu ず  12378  se せ  12379  ze ぜ  12380  so そ  12381  zo ぞ  12382
        |ta た  12383  da だ  12384  ti ち  12385  di ぢ  12386  tU っ  12387  tu つ  12388
        |du づ  12389  te て  12390  de で  12391  to と  12392  do ど  12393  na な  12394
        |ni に  12395  nu ぬ  12396  ne ね  12397  no の  12398  ha は  12399  ba ば  12400
        |pa ぱ  12401  hi ひ  12402  bi び  12403  pi ぴ  12404  hu ふ  12405  bu ぶ  12406
        |pu ぷ  12407  he へ  12408  be べ  12409  pe ぺ  12410  ho ほ  12411  bo ぼ  12412
        |po ぽ  12413  ma ま  12414  mi み  12415  mu む  12416  me め  12417  mo も  12418
        |yA ゃ  12419  ya や  12420  yU ゅ  12421  yu ゆ  12422  yO ょ  12423  yo よ  12424
        |ra ら  12425  ri り  12426  ru る  12427  re れ  12428  ro ろ  12429  wA ゎ  12430
        |wa わ  12431  wi ゐ  12432  we ゑ  12433  wo を  12434  n5 ん  12435  vu ゔ  12436
        |"5 ゛  12443  05 ゜  12444  *5 ゝ  12445  +5 ゞ  12446
        |Katakana
        |a6 ァ  12449  A6 ア  12450  i6 ィ  12451  I6 イ  12452  u6 ゥ  12453  U6 ウ  12454
        |e6 ェ  12455  E6 エ  12456  o6 ォ  12457  O6 オ  12458  Ka カ  12459  Ga ガ  12460
        |Ki キ  12461  Gi ギ  12462  Ku ク  12463  Gu グ  12464  Ke ケ  12465  Ge ゲ  12466
        |Ko コ  12467  Go ゴ  12468  Sa サ  12469  Za ザ  12470  Si シ  12471  Zi ジ  12472
        |Su ス  12473  Zu ズ  12474  Se セ  12475  Ze ゼ  12476  So ソ  12477  Zo ゾ  12478
        |Ta タ  12479  Da ダ  12480  Ti チ  12481  Di ヂ  12482  TU ッ  12483  Tu ツ  12484
        |Du ヅ  12485  Te テ  12486  De デ  12487  To ト  12488  Do ド  12489  Na ナ  12490
        |Ni ニ  12491  Nu ヌ  12492  Ne ネ  12493  No ノ  12494  Ha ハ  12495  Ba バ  12496
        |Pa パ  12497  Hi ヒ  12498  Bi ビ  12499  Pi ピ  12500  Hu フ  12501  Bu ブ  12502
        |Pu プ  12503  He ヘ  12504  Be ベ  12505  Pe ペ  12506  Ho ホ  12507  Bo ボ  12508
        |Po ポ  12509  Ma マ  12510  Mi ミ  12511  Mu ム  12512  Me メ  12513  Mo モ  12514
        |YA ャ  12515  Ya ヤ  12516  YU ュ  12517  Yu ユ  12518  YO ョ  12519  Yo ヨ  12520
        |Ra ラ  12521  Ri リ  12522  Ru ル  12523  Re レ  12524  Ro ロ  12525  WA ヮ  12526
        |Wa ワ  12527  Wi ヰ  12528  We ヱ  12529  Wo ヲ  12530  N6 ン  12531  Vu ヴ  12532
        |KA ヵ  12533  KE ヶ  12534  Va ヷ  12535  Vi ヸ  12536  Ve ヹ  12537  Vo ヺ  12538
        |.6 ・  12539  -6 ー  12540  *6 ヽ  12541  +6 ヾ  12542
        |Bopomofo
        |b4 ㄅ  12549  p4 ㄆ  12550  m4 ㄇ  12551  f4 ㄈ  12552  d4 ㄉ  12553  t4 ㄊ  12554
        |n4 ㄋ  12555  l4 ㄌ  12556  g4 ㄍ  12557  k4 ㄎ  12558  h4 ㄏ  12559  j4 ㄐ  12560
        |q4 ㄑ  12561  x4 ㄒ  12562  zh ㄓ  12563  ch ㄔ  12564  sh ㄕ  12565  r4 ㄖ  12566
        |z4 ㄗ  12567  c4 ㄘ  12568  s4 ㄙ  12569  a4 ㄚ  12570  o4 ㄛ  12571  e4 ㄜ  12572
        |ai ㄞ  12574  ei ㄟ  12575  au ㄠ  12576  ou ㄡ  12577  an ㄢ  12578  en ㄣ  12579
        |aN ㄤ  12580  eN ㄥ  12581  er ㄦ  12582  i4 ㄧ  12583  u4 ㄨ  12584  iu ㄩ  12585
        |v4 ㄪ  12586  nG ㄫ  12587  gn ㄬ  12588
        |Other
        |1c ㈠  12832  2c ㈡  12833  3c ㈢  12834  4c ㈣  12835  5c ㈤  12836  6c ㈥  12837
        |7c ㈦  12838  8c ㈧  12839  9c ㈨  12840  ff ﬀ  64256  fi ﬁ  64257  fl ﬂ  64258
        |ft ﬅ  64261  st ﬆ  64262
        |Custom
        |(0 ⓪  9450   (2 ②  9313   (1 ①  9312
      """.trimMargin()
    )
  }
}
