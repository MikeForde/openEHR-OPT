package com.cabolabs.openehr.rm_1_0_2.common.archetyped

abstract class Pathable {

   String path
   String dataPath
   Pathable parent

   // to be implemented by each pathable class to fill path, dataPath and parent values
   abstract void fillPathable(Pathable parent, String parentAttribute)

/*
   def setParent(Pathable parent)
   {
      this.parent = parent
   }

   def setPath(String path)
   {
      this.path = path
   }

   def setDataPath(String dataPath)
   {
      this.dataPath = dataPath
   }

   def getParent()
   {
      this.parent
   }

   def getPath()
   {
      this.path
   }

   def getDataPath()
   {
      this.dataPath
   }
   */
}
