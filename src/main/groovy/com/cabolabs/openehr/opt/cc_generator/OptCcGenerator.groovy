package com.cabolabs.openehr.opt.cc_generator

import com.cabolabs.openehr.opt.model.AttributeNode
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder

import java.util.jar.JarFile

class OptCcGenerator {

   OperationalTemplate opt
   TerminologyParser terminology

   private static List datavalues = [
      'DV_TEXT', 'DV_CODED_TEXT', 'DV_QUANTITY', 'DV_COUNT',
      'DV_ORDINAL', 'DV_DATE', 'DV_DATE_TIME', 'DV_PROPORTION',
      'DV_DURATION']

   static String PS = File.separator

   private static String frmId = 'FRM056'
   private static String pgId = 'PG23269' //Doesnt matter but valid and will be reassigned by cCube if already used

   private int optId = 2058
   private int initOptId

   // Control location
   private int iCtrlX = 13
   private int iCtrlY = 5

   // Form size
   private int pgHeight = 800

   private Boolean debug = false

   String generate(OperationalTemplate opt)
   {
      this.opt = opt
      this.terminology = TerminologyParser.getInstance()

      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_pt.xml"))


      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes


      // cCube form parameters - these may need to be changed depending on number of controls
      def pgWidth = 600
      

      // Generates HTML while traversing the archetype tree
      builder.EFORMV3(){
         TBLFORMS() {
            FORMID (frmId) 
            TEMPLATEID {}
            FORMNAME (opt.concept)
            CREATEDBY {}
            CREATEDDATE (new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
            MODIFYDATE (new java.text.SimpleDateFormat("dd/MM/yyyy").format(new Date()))
            FONTNAME_F ('Arial')
            FONTSIZE_F ('9')
            PAGEWIDTH (pgWidth) // Need to calculate these based on number of controls in final version
            PAGEHEIGHT (pgHeight) // Need to calculate these based on number of controls in final version
            FORECOLOUR ('16777216')
            BACKCOLOUR ('-1')
            AUTORETAIN_F ('true')
            AUTOSAVE ('false')
            FORMDSN {}
            SETFORNEWUSER {}
            SETFOREXISTUSER {}
            INVALIDUSER {}
            PUBLISHPATH {}
            PDFFILENAME {}
            PDFLOCATION {}
         }
         TBLPAGES() {
            PAGEID (pgId)
            FORMID (frmId)
            PAGENAME(frmId + '_page')
            HELPTEXT {}
            FORECOLOR ('16777216')
            BACKCOLOR ('-1')
            FONTNAME ('Arial')
            FONTSIZE ('9')
            WIDTH (pgWidth) // Need to calculate these based on number of controls in final version
            HEIGHT (pgHeight) // Need to calculate these based on number of controls in final version
            PAGESLNO ('1')
            BACKGROUNDURL {}
            NOTES {}
            SHOWTEMPLATE ('true')
            SETIMPORTID {}
            PAGETYPE ('General')
            AUTORETAIN ('true')
            PAGESIZETYPE ('Custom')
            READONLY ('false')
            BACKSHADING ('Clear')
            SHADINGCOLOUR ('-16777216')
         }
         if(debug) mkp.yieldUnescaped '\n\n  '

         // Three pass process
         // First Pass --> TBLPAGECONTROLCOMMONPROPERTIES - one per control including seperate ones for labels
         initOptId = optId
         println 'Building Common Properties...'
         if(debug) mkp.comment('Common Properties')
         if(debug) mkp.yieldUnescaped '\n  '
         genCtrlCommonProps(opt.definition, builder, opt.definition.archetypeId)

         // Second Pass --> TBLPAGECONTROLPROPERTIES - one per control including seperate ones for labels
         optId = initOptId // Reset optId so matches above
         println 'Building Control Properties...'
         if(debug) mkp.comment('Control Properties')
         if(debug) mkp.yieldUnescaped '\n  '
         genCtrlProps(opt.definition, builder, opt.definition.archetypeId)

         // Third Pass --> TBLCONTROLVALIDATION - only for DV_QUANTITY and DV_COUNT
         optId = initOptId // Reset optId so matches above
         println 'Building Validation Elements for Quantity and Count (if present)...'
         if(debug) mkp.comment('Validation Elements for Quantity and Count')
         if(debug) mkp.yieldUnescaped '\n  '
         genCtrlValid(opt.definition, builder, opt.definition.archetypeId)
      }

      return writer.toString()
   }

   void genCtrlCommonProps(ObjectNode o, MarkupBuilder b, String parent_arch_id)
   {
      //println "genCtrlCommonProps Called"
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      // support for non ELEMENT.value fields that are in the OPT
      // TODO: support for IM fields that are not in the OPT like INSTRUCTION.narrative
      // if (datavalues.contains(o.rmTypeName))
      // {
      //    genCtrlCommonPropsFields(o, b, parent_arch_id)
      //    return
      // }

      // Calculated position controls
      def normOptId = optId - 2063
   	// def ctrlX = 13
      // def ctrlY = 9 + ((iCtrlY - 1) * 26)
      


      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         // println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)
         // println "name variable = " + name
         // println "value variable = " + value

         String sExtraLabel = ''

         

         if (name) {
            //genCtrlCommonPropsFields(name, b, parent_arch_id)
         }
         else
         {
            if (value) {
               iCtrlY = iCtrlY + 26
               sExtraLabel = genCtrlCommonPropsFields(value, b, parent_arch_id)
               if(debug) b.mkp.yieldUnescaped '\n\n  '
            }

            iCtrlY = iCtrlY + 26
            if(debug) b.mkp.comment('Label')

            // Calculate required width of label
            def sLabel = opt.getTerm(parent_arch_id, o.nodeId) + sExtraLabel
            def sUpperCase = ((sLabel =~ /[A-Z]/).findAll()).join()
            def sLowerCase = ((sLabel =~ /[a-z0-9]/).findAll()).join()
            def iLabelWidth = (12 * sUpperCase.length()) + (8 * sLowerCase.length())
            
            if(debug) {
               println sLabel + " - " + iLabelWidth
            } else {
               println sLabel
            }    

            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               // Label
               OPTIONID ('OPT' + (optId))
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('Label' + normOptId)
               CONTROLTYPE ('8')
               PARENTCONTROLID (pgId)
               TEXT(sLabel)
               XPOSITION (iCtrlX)
               YPOSITION (iCtrlY - 52)
               mkp.yieldUnescaped cCubeStandardCommonXML1()
               WIDTH(iLabelWidth)
               mkp.yieldUnescaped cCubeStandardCommonXML2()
               TABINDEX(normOptId)
               mkp.yieldUnescaped cCubeStandardCommonXML3()
               CSSSTYLE('eformLabel ')
               mkp.yieldUnescaped cCubeStandardCommonXML4()
            }
            if(debug) b.mkp.yieldUnescaped '\n\n\n  '

            
            if(iCtrlY > (pgHeight- 52)){
               iCtrlY = 5
               iCtrlX = iCtrlX + 150
            }
         }

         return
      }

      if (o.type == "ARCHETYPE_SLOT")
      {
         // b.div(class: o.rmTypeName +'  form-item') {
         //    label("ARCHETYPE_SLOT is not supported yet, found at "+ o.path)
         // }
         return // Generator do not support slots on OPTs
      }

      // ***********************************************************************
      //
      // Here we need to avoid processing attributes that should be set
      // internally (not by a user) like ACTIVITY.action_archetype_id or
      // INSTRUCTION_DETAILS.instruction_id or INSTRUCTION_DETAILS.activity_id
      // and expose openEHR IM attributes that are not in the OPT, like
      // ACTION.time or INSTRUCTION.expiry_tyime or INSTRUCTION.narrative
      //
      // ***********************************************************************

      // Process all non-ELEMENTs
      

      // label for intermediate nodes
      def term = opt.getTerm(parent_arch_id, o.nodeId)

      //println o.path

      o.attributes.each { attr ->

         // Sample avoid ACTIVITY.action_archetype_id
         // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
         if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
         if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
         if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return

         genCtrlCommonProps(attr, b, parent_arch_id)
      }
   }

   void genCtrlCommonProps(AttributeNode a, MarkupBuilder b, String parent_arch_id)
   {
      a.children.each {
         optId++
         genCtrlCommonProps(it, b, parent_arch_id)
      }
   }

   // TODO: refactor in different functions
   String genCtrlCommonPropsFields(ObjectNode node, MarkupBuilder b, String parent_arch_id)
   {
      // Calculated position controls
      def normOptId = optId - 2063
      String rtnForLabel = ''

      switch (node.rmTypeName)
      {
         case 'DV_TEXT':
         case 'DV_QUANTITY':
         case 'DV_COUNT':
            //builder.textarea(class: node.rmTypeName +' form-control', name:node.path, '')
            optId++
            printf node.rmTypeName + ": "
            if(debug) b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               OPTIONID ('OPT' + (optId-1))
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('TextBox' + normOptId)
               CONTROLTYPE ('15')
               PARENTCONTROLID (pgId)
               TEXT('')
               XPOSITION (iCtrlX)
               YPOSITION (iCtrlY)
               mkp.yieldUnescaped cCubeStandardCommonXML1()
               WIDTH('120') // will need changing depending on text size
               mkp.yieldUnescaped cCubeStandardCommonXML2()
               TABINDEX(normOptId)
               mkp.yieldUnescaped cCubeStandardCommonXML3()
               CSSSTYLE('eformTextBox ')
               mkp.yieldUnescaped cCubeStandardCommonXML4()
            }
         break
         case 'DV_CODED_TEXT':
         case 'DV_ORDINAL':
            optId++
            printf node.rmTypeName + ": "
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               OPTIONID ('OPT' + (optId-1))
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('DropDownList' + normOptId)
               CONTROLTYPE ('4')
               PARENTCONTROLID (pgId)
               TEXT {}
               XPOSITION (iCtrlX)
               YPOSITION (iCtrlY)
               mkp.yieldUnescaped cCubeStandardCommonDropBoxXML()
               WIDTH('120') // will need changing depending on text size
               mkp.yieldUnescaped cCubeStandardCommonXML2()
               TABINDEX(normOptId)
               mkp.yieldUnescaped cCubeStandardCommonXML3()
               CSSSTYLE('eformDropDownList ')
               mkp.yieldUnescaped cCubeStandardCommonXML4()
            }
         break
         // case 'DV_TIME':
         //    builder.input(type:'time', name:node.path, class: node.rmTypeName +' form-control')
         // break
         case 'DV_DATE':
         case 'DV_DATE_TIME':
            optId++
            printf node.rmTypeName + ": "
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               OPTIONID ('OPT' + (optId-1))
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('Calendar' + normOptId)
               CONTROLTYPE ('2')
               PARENTCONTROLID (pgId)
               TEXT {}
               XPOSITION (iCtrlX)
               YPOSITION (iCtrlY)
               mkp.yieldUnescaped cCubeStandardCommonXML1()
               WIDTH('120') // will need changing depending on text size
               mkp.yieldUnescaped cCubeStandardCommonXML2()
               TABINDEX(normOptId)
               mkp.yieldUnescaped cCubeStandardCommonXML3()
               CSSSTYLE('eformCalendar ')
               mkp.yieldUnescaped cCubeStandardCommonXML4()
            }
         break
         // case 'DV_DATE_TIME':
         //    builder.input(type:'datetime-local', name:node.path, class: node.rmTypeName +' form-control')
         // break
         case 'DV_BOOLEAN':
            optId++
            printf node.rmTypeName + ": "
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLCOMMONPROPERTIES() {
               OPTIONID ('OPT' + (optId-1))
               PAGEID (pgId)
               TEMPLATEID {}
               FORMID (frmId)
               CONTROLID('CheckBox' + normOptId)
               CONTROLTYPE ('3')
               PARENTCONTROLID (pgId)
               TEXT ('CheckBox' + normOptId)
               XPOSITION (iCtrlX)
               YPOSITION (iCtrlY)
               mkp.yieldUnescaped cCubeStandardCommonXML1()
               WIDTH('120') // will need changing depending on text size
               mkp.yieldUnescaped cCubeStandardCommonXML2()
               TABINDEX(normOptId)
               mkp.yieldUnescaped cCubeStandardCommonXML3()
               CSSSTYLE('eformCheckBox ')
               mkp.yieldUnescaped cCubeStandardCommonXML4()
            }
         break
         default: // TODO: generar campos para los DV_INTERVAL
            println "Datatype "+ node.rmTypeName +" not supported yet"
      }

      // Create rtnForLabel
      switch (node.rmTypeName)
      {
         case 'DV_QUANTITY':
            if (node.list.size() == 0)
               {
                  // input(type:'text', name:node.path+'/units', class: node.rmTypeName +' form-control')
               }
               else
               {
                  node.list.units.each { u ->

                     rtnForLabel = rtnForLabel + ' Units: ' + u
                  }
               }
         break
         case 'DV_COUNT':
            def rangeNode = node.attributes.children.item.range[0]
            def lowerRng
            def upperRng
            if (rangeNode != null) {
               lowerRng= rangeNode.lower[0] == null ? 0 : rangeNode.lower[0]
               upperRng= rangeNode.upper[0] == null ? 0 : rangeNode.upper[0]
               rtnForLabel = ' Range('+ lowerRng + '-' + upperRng + ')'
            } else {
               rtnForLabel = ''
            }
            
         break
         default:
            rtnForLabel = ''
      }


      return rtnForLabel
   }

   def cCubeStandardCommonXML1()
   {
      def xmlText = '''
    <FONTNAME>Arial</FONTNAME>
    <FONTSIZE>9</FONTSIZE>
    <BOLD>false</BOLD>
    <ITALIC>false</ITALIC>
    <UNDERLINE>false</UNDERLINE>
    <STRIKEOUT>false</STRIKEOUT>
    <FORECOLOUR>16777216</FORECOLOUR>
    <BACKCOLOUR>-1</BACKCOLOUR>
    <BORDERCOLOUR>-16777216</BORDERCOLOUR>
    <BORDERSTYLE>NotSet</BORDERSTYLE>
    <BORDERWIDTH>0</BORDERWIDTH>
    <HEIGHT>23</HEIGHT>'''
      return xmlText
   }

   def cCubeStandardCommonDropBoxXML()
   {
      def xmlText = '''
    <FONTNAME>Arial</FONTNAME>
    <FONTSIZE>9</FONTSIZE>
    <BOLD>false</BOLD>
    <ITALIC>false</ITALIC>
    <UNDERLINE>false</UNDERLINE>
    <STRIKEOUT>false</STRIKEOUT>
    <FORECOLOUR>16777216</FORECOLOUR>
    <BACKCOLOUR>-1</BACKCOLOUR>
    <BORDERCOLOUR>-16777216</BORDERCOLOUR>
    <BORDERSTYLE />
    <BORDERWIDTH>0</BORDERWIDTH>
    <HEIGHT>23</HEIGHT>'''
      return xmlText
   }

   def cCubeStandardCommonXML2()
   {
      def xmlText = '''
    <HOLDVALUE>false</HOLDVALUE>
    <READONLY>false</READONLY>'''
      return xmlText
   }

   def cCubeStandardCommonXML3()
   {
      def xmlText = '''
    <VISIBLE>true</VISIBLE>
    <DISPLAY>true</DISPLAY>
    <HELPTEXT />
    <TOOLTIP />'''
      return xmlText
   }

   def cCubeStandardCommonXML4()
   {
      def xmlText = '''
    <BACKSHADING>Clear</BACKSHADING>
    <SHADINGCOLOUR>-16777216</SHADINGCOLOUR>
    <ZINDEX>0</ZINDEX>'''
      return xmlText
   }

  void genCtrlProps(ObjectNode o, MarkupBuilder b, String parent_arch_id)
   {
      //println "genCtrlProps Called"
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      // Calculated position controls
      def normOptId = optId - 2063
   	def ctrlX = 13
      def ctrlY = 9 + (normOptId * 50)


      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         // println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)
         // println "name variable = " + name
         // println "value variable = " + value

         if (name) {
            //genCtrlPropsFields(name, b, parent_arch_id)
         }
         else
         {
            if (value) {
               genCtrlPropsFields(value, b, parent_arch_id)
               if(debug)  b.mkp.yieldUnescaped '\n\n '
            }

            if(debug)  b.mkp.comment('Label')
            b.TBLPAGECONTROLPROPERTIES() {
               // Label
               PROPERTYID('CTRLPR' + optId)
               OPTIONID ('OPT' + optId)
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardLabelXML()
            }
            if(debug)  b.mkp.yieldUnescaped '\n\n\n  '
         }
         

         return
      }

      if (o.type == "ARCHETYPE_SLOT")
      {
         // b.div(class: o.rmTypeName +'  form-item') {
         //    label("ARCHETYPE_SLOT is not supported yet, found at "+ o.path)
         // }
         return // Generator do not support slots on OPTs
      }

      // Process all non-ELEMENTs
      

      // label for intermediate nodes
      def term = opt.getTerm(parent_arch_id, o.nodeId)

      //println o.path

      o.attributes.each { attr ->

         // Sample avoid ACTIVITY.action_archetype_id
         // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
         if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
         if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
         if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return

         genCtrlProps(attr, b, parent_arch_id)
      }
   }

   void genCtrlProps(AttributeNode a, MarkupBuilder b, String parent_arch_id)
   {
      a.children.each {
         optId++
         genCtrlProps(it, b, parent_arch_id)
      }
   }

   // TODO: refactor in different functions
   void genCtrlPropsFields(ObjectNode node, MarkupBuilder b, String parent_arch_id)
   {
      // Calculated position controls
      //def normOptId = optId - 2063

      switch (node.rmTypeName)
      {
         case 'DV_TEXT':
            //builder.textarea(class: node.rmTypeName +' form-control', name:node.path, '')
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardTextboxXML()
            }
         break
         case 'DV_CODED_TEXT': // Dropdown
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            // Get the dropdown items
            def constraint = node.attributes.find{ it.rmAttributeName == 'defining_code' }.children[0]
            def listItems = []
            constraint.codeList.each { code_node ->
                             listItems.add(opt.getTerm(parent_arch_id, code_node))
                           }
            def listItemsAsString = listItems.join(",")
            // Build xml
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardDropDownXML1()
               ITEMSCOLLECTION(listItemsAsString)
               mkp.yieldUnescaped cCubeStandardDropDownXML2()
            }
         break
         case 'DV_ORDINAL': // Dropdown
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            // Get the dropdown items
            def listItems = []
            def ordinals = [] 
            node.list.each { ord ->
   	            ordinals.add(ord.value)
                  listItems.add(opt.getTerm(parent_arch_id, ord.symbol.codeString))
                  }
            def listItemsAsString = listItems.join(",")
            def ordinalsAsString = ordinals.join(",")
            // Build xml
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardDropDownXML1()
               ITEMSCOLLECTION(listItemsAsString + ':' + ordinalsAsString)
               mkp.yieldUnescaped cCubeStandardDropDownXML2()
            }
         break
         case 'DV_QUANTITY':
         case 'DV_COUNT':
            //builder.textarea(class: node.rmTypeName +' form-control', name:node.path, '')
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardQuantCountTextboxXML()
            }
         break
         // case 'DV_TIME':
         //    builder.input(type:'time', name:node.path, class: node.rmTypeName +' form-control')
         // break
         case 'DV_DATE':
         case 'DV_DATE_TIME':
            optId++
            if(debug) println "DV_DATE"
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardCalendarXML()
            }
         break
         // case 'DV_DATE_TIME':
         //    builder.input(type:'datetime-local', name:node.path, class: node.rmTypeName +' form-control')
         // break
         case 'DV_BOOLEAN':
            optId++
            if(debug) println "DV_BOOLEAN"
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLPAGECONTROLPROPERTIES() {
               PROPERTYID('CTRLPR' + (optId-1))
               OPTIONID ('OPT' + (optId-1))
               FORMID (frmId)
               mkp.yieldUnescaped cCubeStandardCheckBoxXML()
            }
         break
         default: // TODO: generar campos para los DV_INTERVAL
            println "Datatype "+ node.rmTypeName +" not supported yet"
      }
   }

   def cCubeStandardXML1()
   {
      def xmlText = '''
    <IMAGE />
    <IMAGEURL />
    <TARGET />'''
      return xmlText
   }

   def cCubeStandardDropDownXML1()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>0</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE xml:space="preserve">          </TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>false</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>false</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />'''
      return xmlText
   }

   def cCubeStandardDropDownXML2()
   {
      def xmlText = '''
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION />
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN />
    <DIRECTION />
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION>Not Include</BLANKOPTION>
    <SELECTOPTION>Display</SELECTOPTION>
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE />
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN />
    <TOOLBARMODE />
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   def cCubeStandardLabelXML()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>0</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE xml:space="preserve">          </TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>true</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>false</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />
    <ITEMSCOLLECTION />
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION />
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN>MiddleLeft</TEXTALIGN>
    <DIRECTION />
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION />
    <SELECTOPTION />
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE />
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN />
    <TOOLBARMODE />
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   def cCubeStandardTextboxXML()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>50</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE>SingleLine</TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>true</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>false</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />
    <ITEMSCOLLECTION />
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION />
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN>Left</TEXTALIGN>
    <DIRECTION />
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION />
    <SELECTOPTION />
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE />
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN>Default</SKIN>
    <TOOLBARMODE>Default</TOOLBARMODE>
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   def cCubeStandardQuantCountTextboxXML()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>50</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE>SingleLine</TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>true</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>true</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />
    <ITEMSCOLLECTION />
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION>Set</VALIDATION>
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN>Left</TEXTALIGN>
    <DIRECTION />
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION />
    <SELECTOPTION />
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE />
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN>Default</SKIN>
    <TOOLBARMODE>Default</TOOLBARMODE>
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   def cCubeStandardCalendarXML()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>0</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE xml:space="preserve">          </TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>true</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>false</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />
    <ITEMSCOLLECTION />
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION />
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN />
    <DIRECTION />
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION />
    <SELECTOPTION />
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE />
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN />
    <TOOLBARMODE />
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   def cCubeStandardCheckBoxXML()
   {
      def xmlText = '''
    <CURRENCY />
    <MAXLENGTH>0</MAXLENGTH>
    <IMAGE />
    <IMAGEURL />
    <TARGET />
    <TEXTMODE xml:space="preserve">          </TEXTMODE>
    <ALLOWPAGING>false</ALLOWPAGING>
    <AUTOSUGGEST>false</AUTOSUGGEST>
    <CAUSEVALIDATION>true</CAUSEVALIDATION>
    <DISPLAYIMAGE>true</DISPLAYIMAGE>
    <PDF>false</PDF>
    <PREVIEWPDF>false</PREVIEWPDF>
    <RECORDSPERPAGE>1</RECORDSPERPAGE>
    <REQUIRED>false</REQUIRED>
    <SHOWCURRENTDATE>false</SHOWCURRENTDATE>
    <SHOWPDF>false</SHOWPDF>
    <BACKGROUNDIMAGE />
    <BACKIMAGEURL />
    <BUTONTYPE />
    <EMAIL />
    <ITEMSCOLLECTION />
    <NAVIGATEURL />
    <NAVIGATION />
    <OTHERDATASOURCE />
    <CALCULATION />
    <VALIDATION />
    <ALERTCONDITION />
    <FUNCTIONORDER />
    <GRIDROWHEIGHT>0</GRIDROWHEIGHT>
    <GRIDLINECOLOR />
    <PDFPAGEID />
    <GRIDBLANKROW>false</GRIDBLANKROW>
    <TEXTALIGN />
    <DIRECTION>Horizontal</DIRECTION>
    <GRIDCOLUMNS />
    <GRIDCOLUMNBINDINGS />
    <GRIDCOLUMNCHARTBINDINGS />
    <PDFCONFIGURATION />
    <SETFORNEWUSER />
    <SETFOREXIST />
    <WEBAPICONFIG />
    <CONTROLROTATION>0</CONTROLROTATION>
    <PDFORIENTATION />
    <CONDTIONALFORMATTING />
    <X1POSITION>0</X1POSITION>
    <Y1POSITION>0</Y1POSITION>
    <X2POSITION>0</X2POSITION>
    <Y2POSITION>0</Y2POSITION>
    <INVERT />
    <CHARTSETTING />
    <CHARTCONFIGURATION />
    <SLIDERBARCOLOUR />
    <SLIDERBARFIRSTNO>0</SLIDERBARFIRSTNO>
    <SLIDERBARENDNO>0</SLIDERBARENDNO>
    <SLIDERBARINCREMENTS>0</SLIDERBARINCREMENTS>
    <SLIDERBARORIENTATION />
    <SLIDERBARTOOLTIP>false</SLIDERBARTOOLTIP>
    <SLIDERSTRETCHTOFIT>false</SLIDERSTRETCHTOFIT>
    <PICKERDISPLAYTYPE />
    <PICKERMODE />
    <PICKERTHEME />
    <PICKERSHOWCURRENT>false</PICKERSHOWCURRENT>
    <SLIDERBARSHOWINCREMENTLABELS>false</SLIDERBARSHOWINCREMENTLABELS>
    <BLANKOPTION />
    <SELECTOPTION />
    <FORMAT />
    <SHORTCUTKEY />
    <DEFAULTVALUE>True</DEFAULTVALUE>
    <SHOWSCROLL>true</SHOWSCROLL>
    <CONTROLACTION />
    <POSITIONING>Fixed</POSITIONING>
    <OTHER>false</OTHER>
    <OTHERDEFAULTVALUE>999</OTHERDEFAULTVALUE>
    <RUNJAVASCRIPT />
    <SUBFORMLINK />
    <ENABLEQUERYSTRING>true</ENABLEQUERYSTRING>
    <SKIN />
    <TOOLBARMODE />
    <TOOLS />
    <TRACKCHANGES>false</TRACKCHANGES>'''
      return xmlText
   }

   void genCtrlValid(ObjectNode o, MarkupBuilder b, String parent_arch_id)
   {
      //println "genCtrlValid Called"
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      // Calculated position controls
      // def normOptId = optId - 2063
   	// def ctrlX = 13
      // def ctrlY = 9 + (normOptId * 50)


      if (o.rmTypeName == "ELEMENT")
      {
         // constraints for ELEMENT.name and ELEMENT.value, can be null
         // uses the first alternative (these are single attributes and can have alternative constraints)
         def name = o.attributes.find { it.rmAttributeName == 'name' }?.children?.getAt(0)
         def value = o.attributes.find { it.rmAttributeName == 'value' }?.children?.getAt(0)

         // println "element name "+ opt.getTerm(parent_arch_id, o.nodeId)
         // println "name variable = " + name
         // println "value variable = " + value

         if (name) {
            //genCtrlValidFields(name, b, parent_arch_id)
         }
         else
         {
            //if(debug)  b.mkp.comment('Label')
            //if(debug)  b.mkp.yieldUnescaped '\n\n  '

            if (value) {
               genCtrlValidFields(value, b, parent_arch_id)
            }
         }
         

         return
      }

      if (o.type == "ARCHETYPE_SLOT")
      {
         // b.div(class: o.rmTypeName +'  form-item') {
         //    label("ARCHETYPE_SLOT is not supported yet, found at "+ o.path)
         // }
         return // Generator do not support slots on OPTs
      }

      // Process all non-ELEMENTs
      

      // label for intermediate nodes
      def term = opt.getTerm(parent_arch_id, o.nodeId)

      //println o.path

      o.attributes.each { attr ->

         // Sample avoid ACTIVITY.action_archetype_id
         // This can be done in a generic way by adding a mapping rmTypeName -> rmAttributeNames
         if (o.rmTypeName == 'ACTIVITY' && attr.rmAttributeName == 'action_archetype_id') return
         if (o.rmTypeName == 'COMPOSITION' && attr.rmAttributeName == 'category') return
         if (o.rmTypeName == 'ACTION' && attr.rmAttributeName == 'ism_transition') return

         genCtrlValid(attr, b, parent_arch_id)
      }
   }

   void genCtrlValid(AttributeNode a, MarkupBuilder b, String parent_arch_id)
   {
      a.children.each {
         optId++
         genCtrlValid(it, b, parent_arch_id)
      }
   }

   // TODO: refactor in different functions
   void genCtrlValidFields(ObjectNode node, MarkupBuilder b, String parent_arch_id)
   {
      // Calculated position controls
      def normOptId = optId - 2063
   	// def ctrlX = 13
      // def ctrlY = 9 + (normOptId * 50)

      switch (node.rmTypeName)
      {
         case 'DV_TEXT':
         case 'DV_CODED_TEXT': // Dropdown
         case 'DV_ORDINAL': // Dropdown
         case 'DV_DATE':
         case 'DV_DATE_TIME':
         case 'DV_BOOLEAN':
            optId++
            //println node.rmTypeName
            //if(debug)  b.mkp.comment(node.rmTypeName)
         break
         case 'DV_QUANTITY':
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            b.TBLCONTROLVALIDATION() {
               CONTROLVALIDATIONID('CVALID0' + (20 + normOptId))
               FORMID (frmId)
               OPTIONID ('OPT' + (optId-1))
               REQUIRED('true')
               REQUIREDERRORMESSAGE {}
               MINVALUE {}
               MAXVALUE {}
               RANGEDATATYPE('Select')
               RANGEERRORMESSAGE {}
               REGULAREXPRESSIONFORMULA('^\\d+$')
               REGULARERRORMESSAGE('Only digits are allowed.')
            }
            if(debug)  b.mkp.yieldUnescaped '\n\n\n  '
         break
         case 'DV_COUNT':
            optId++
            if(debug) println node.rmTypeName
            if(debug)  b.mkp.comment(node.rmTypeName)
            //println node.attributes.children.item.range.lower
            //def range = node.attributes.find{ it.rmAttributeName == 'defining_code' }.children[0]
            def rangeNode = node.attributes.children.item.range[0]
            def lowerRng
            def upperRng
            if (rangeNode != null) {
               lowerRng= rangeNode.lower[0] == null ? 0 : rangeNode.lower[0]
               upperRng = rangeNode.upper[0] == null ? 0 : rangeNode.upper[0]
            } else {
               lowerRng= 0
               upperRng = 0
            }
            b.TBLCONTROLVALIDATION() {
               CONTROLVALIDATIONID('CVALID0' + (20 + normOptId))
               FORMID (frmId)
               OPTIONID ('OPT' + (optId-1))
               REQUIRED('true')
               REQUIREDERRORMESSAGE {}
               MINVALUE (lowerRng)
               MAXVALUE (upperRng)
               RANGEDATATYPE('Integer')
               RANGEERRORMESSAGE ('Out of range')
               REGULAREXPRESSIONFORMULA('^\\d+$')
               REGULARERRORMESSAGE('Only digits are allowed.')
            }
            if(debug)  b.mkp.yieldUnescaped '\n\n\n  '
         break
         // case 'DV_TIME':
         //    builder.input(type:'time', name:node.path, class: node.rmTypeName +' form-control')
         // break
         // case 'DV_DATE_TIME':
         //    builder.input(type:'datetime-local', name:node.path, class: node.rmTypeName +' form-control')
         // break
         default: // TODO: generar campos para los DV_INTERVAL
            println "Datatype "+ node.rmTypeName +" not supported yet"
      }
   }
}
