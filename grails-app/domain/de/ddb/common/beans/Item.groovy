/*
 * Copyright (C) 2013 FIZ Karlsruhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.common.beans

class Item {

    static constraints = {
    }

    String id
    String parent
    String label
    String type
    String position
    String leaf
    String aggregationEntity
    List<Item> children = []
    Item parentItem

    Item(){
    }

    Item(Map itemMap){
        id = itemMap.id
        parent = itemMap.parent
        label = itemMap.label
        type = itemMap.type
        position = itemMap.position
        leaf = itemMap.leaf
        aggregationEntity = itemMap.aggregationEntity
    }

    public String toString(){
        return "Item["+this.id+"]"
    }

    public boolean hasChildren(){
        if(children.size()>0){
            return true;
        }else{
            return false
        }
    }

    public List<Item> getChildren(){
        return children;
    }


    public void addItemsToHierarchy(List itemListJson) {
        List<Item> allItemList = []
        itemListJson.each {
            allItemList.add(new Item(it))
        }

        for(int i=0; i<allItemList.size(); i++){
            Item currentItem = allItemList.get(i)
            Item parent = this.getItemFromHierarchy(currentItem.parent)
            if(parent != null){
                parent.children.add(currentItem)
                currentItem.parentItem = parent
            }
        }
    }

    public Item getItemFromHierarchy(String id){
        if(this.id == id){
            return this
        }else{
            for(int i=0; i<this.children.size(); i++){
                Item currentChild = this.children.get(i)
                Item foundItem = currentChild.getItemFromHierarchy(id)
                if(foundItem != null){
                    return foundItem
                }
            }
        }
    }

    public void removeItemFromHierarchy(String id){
        for(int i=0; i<this.children.size(); i++){
            Item currentChild = this.children.get(i)
            if(currentChild.id == id){
                currentChild.parent = null
                children.remove(currentChild)
                return
            }
            currentChild.removeItemFromHierarchy(id)
        }
    }

    static boolean doesParentListContainHierarchy(String id, List allItemsJson){
        if(allItemsJson == null){
            return false
        }
        if(allItemsJson.size() <= 1){
            return false
        }
        if(allItemsJson.size()>1){
            return true
        }
        return false;
    }

    static Item buildHierarchy(List allItemsJson){
        List<Item> allItemList = []
        allItemsJson.each {
            allItemList.add(new Item(it))
        }

        Item root = getRootItem(allItemList)
        allItemList.remove(root)

        List<Item> currentParents = [root]
        while(allItemList.size() > 0){
            List<Item> appendedChilds = appendChildren(currentParents, allItemList)
            currentParents = appendedChilds;
        }
        return root
    }

    private static Item getRootItem(List allItems) {
        for(int i=0; i<allItems.size(); i++){
            Item currentItem = allItems.get(i)
            if(currentItem.parent == null || "null".equals(currentItem.parent) ){
                return currentItem
            }
        }
        return null
    }

    private static List<Item> appendChildren(List<Item> parents, List possibleChildList){
        List<Item> addedChilds = []
        for(int i=0; i<parents.size(); i++){
            Item currentParent = parents.get(i)
            for(int j=possibleChildList.size()-1; j>=0; j--){
                Item currentChild = possibleChildList.get(j)
                if(currentChild.parent.equals(currentParent.id)){
                    currentParent.children.add(currentChild)
                    currentChild.parentItem = currentParent
                    possibleChildList.remove(j)
                    addedChilds.add(currentChild)
                }
            }
        }
        return addedChilds
    }
}
