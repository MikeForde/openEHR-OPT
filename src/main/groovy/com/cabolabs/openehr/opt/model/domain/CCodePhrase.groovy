package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

@groovy.util.logging.Log4j
class CCodePhrase extends ObjectNode {

   // CODE LIST CONSTRAINT

   // List<String>
   List codeList = []
   String terminologyId


   // REFERENCE SET URI CONSTRAINT

   // 1. This is mapped to the element referenceSetUri
   // 2. By the XSD, referenceSetUri is on a subclass of C_CODE_PHRASE, C_CODE_REFERENCE
   // 3. There are OPTs with C_CODE_PHRASE having referenceSetUri
   // 4. We parse both C_CODE_PHRASE and C_CODE_REFERENCE to CCodePhrase to avoid problems that come from modeling tools

   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   String terminologyRef

   // CONSTRAINT_REF
   String reference

   /**
    * @param terminologyId format 'name[(version)]'
    */
   ValidationResult isValid(Pathable parent, String code, String terminologyId)
   {
      // TODO: search for the terminologyId in the ref
      if (this.terminologyRef) return new ValidationResult(isValid: true)

      // if there is no list, any code is valid
      if (this.codeList)
      {
         def item = this.codeList.find { it == code }
         if (!item) return new ValidationResult(isValid: false, message: "code_string ${code} is not in the code list "+ codeList)
      }

      if (this.terminologyId)
      {
         log.info("CCodePhrase isValid")
         
         if (terminologyId != this.terminologyId)
         {
            return new ValidationResult(isValid: false, message: "terminology_id ${terminologyId} doesn't match ${this.terminologyId}")
         }
      }

      return new ValidationResult(isValid: true)
   }
}
