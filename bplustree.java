import java.lang.*;
import java.util.*;
import java.io.*;

public class bplustree {
    int m;
    InternalNode root;
    LeafNode first_Leaf;

                          //Supporting functions
    //This function searches for a key with in list of dictionary pair
    //Returns value of given key if the pair is present . Otheriwse returns -1 .
    private int binary_Search(DictionaryPair[] dpairs, int numofPairs, int target) {
        Comparator<DictionaryPair> c = new Comparator<DictionaryPair>() {
            @Override
            public int compare(DictionaryPair o1, DictionaryPair o2) {
                Integer a = Integer.valueOf(o1.key);
                Integer b = Integer.valueOf(o2.key);
                return a.compareTo(b);
            }
        };
        return Arrays.binarySearch(dpairs, 0, numofPairs, new DictionaryPair(target, 0), c);
    }


    //This function return the leafnode that contain the pair with given key in its dictionary pairs
    // by traversing from root through the internal nodes.
    private LeafNode find_LeafNode(int key) {

        // Initialize keys and index variable
        Integer[] keys = this.root.keys;
        int i;

        //To traverse to the leaf node by finding nodes in path to it
        for (i = 0; i < this.root.degree - 1; i++) {
            if (key < keys[i]) { break; }
        }

		//Check if the node is the leaf node
        Node child = this.root.childPointers[i];
        if (child instanceof LeafNode) {
            return (LeafNode)child;
        }
        //To continue to search a level down
        else {
            return find_LeafNode((InternalNode)child, key);
        }
    }

    private LeafNode find_LeafNode(InternalNode node, int key) {


        Integer[] keys = node.keys;
        int i;

        for (i = 0; i < node.degree - 1; i++) {
            if (key < keys[i]) { break; }
        }

        Node childNode = node.childPointers[i];
        if (childNode instanceof LeafNode) {
            return (LeafNode)childNode;
        } else {
            return find_LeafNode((InternalNode)node.childPointers[i], key);
        }
    }

    //This function returns the index of the leafnode by looking through the given list of pointers.
    private int find_IndexOfPointer(Node[] pointers, LeafNode node) {
        int i;
        for (i = 0; i < pointers.length; i++) {
            if (pointers[i] == node) { break; }
        }
        return i;
    }

    //This function is used to compute the mid point of the max degree of the B+ tree .
    private int getMidpoint() {
        return (int)Math.ceil((this.m + 1) / 2.0) - 1;
    }

    //Function to check if the B+ tree is empty or not.
    private boolean isEmpty() {
        return first_Leaf == null;
    }

    //This function returns the index that contains first null entry in it
    private int linearNullSearch(DictionaryPair[] dps) {
        for (int i = 0; i <  dps.length; i++) {
            if (dps[i] == null) { return i; }
        }
        return -1;
    }

    //This fucntion handles deficieny in a given internal node by borrowing and merging .
    private void handle_Deficiency(InternalNode in) {

        InternalNode sibling;
        InternalNode parent = in.parent;

        // Remedies deficient root node
        if (this.root == in) {
            for (int i = 0; i < in.childPointers.length; i++) {
                if (in.childPointers[i] != null) {
                    if (in.childPointers[i] instanceof InternalNode) {
                        this.root = (InternalNode)in.childPointers[i];
                        this.root.parent = null;
                    } else if (in.childPointers[i] instanceof LeafNode) {
                        this.root = null;
                    }
                }
            }
        }

        // Borrow:
        else if (in.leftSibling != null && in.leftSibling.isLendable()) {
            sibling = in.leftSibling;
        } else if (in.rightSibling != null && in.rightSibling.isLendable()) {
            sibling = in.rightSibling;

            // Copy 1 key and pointer from sibling
            int borrowedKey = sibling.keys[0];
            Node pointer = sibling.childPointers[0];

            // Copy root key and pointer into parent
            in.keys[in.degree - 1] = parent.keys[0];
            in.childPointers[in.degree] = pointer;

            // Copy borrowedKey into root
            parent.keys[0] = borrowedKey;

            // Delete key and pointer from sibling
            sibling.removePointer(0);
            Arrays.sort(sibling.keys);
            sibling.removePointer(0);
            shiftDown(in.childPointers, 1);
        }

        // Merge:
        else if (in.leftSibling != null && in.leftSibling.isMergeable()) {

        } else if (in.rightSibling != null && in.rightSibling.isMergeable()) {
            sibling = in.rightSibling;

            // Copy rightmost key in parent to beginning of sibling's keys &
            // delete key from parent
            sibling.keys[sibling.degree - 1] = parent.keys[parent.degree - 2];
            Arrays.sort(sibling.keys, 0, sibling.degree);
            parent.keys[parent.degree - 2] = null;

            // Copy in's child pointer over to sibling's list of child pointers
            for (int i = 0; i < in.childPointers.length; i++) {
                if (in.childPointers[i] != null) {
                    sibling.prependChildPointer(in.childPointers[i]);
                    in.childPointers[i].parent = sibling;
                    in.removePointer(i);
                }
            }

            // Delete child pointer from grandparent to deficient node
            parent.removePointer(in);

            // Remove left sibling
            sibling.leftSibling = in.leftSibling;
        }

        // Handle deficiency a level up if it exists
        if (parent != null && parent.isDeficient()) {
            handle_Deficiency(parent);
        }
    }



    //This function does linear search on the given list of nodes and returns the index of the first null entry,
    // Otherwise returns a -1 .

    private int linearNullSearch(Node[] pointers) {
        for (int i = 0; i <  pointers.length; i++) {
            if (pointers[i] == null) { return i; }
        }
        return -1;
    }

    //This function is used to sort given list of dictionary pairs along with interspread nulls.
    private void sortDictionary(DictionaryPair[] dictionary) {
        Arrays.sort(dictionary, new Comparator<DictionaryPair>() {
            @Override
            public int compare(DictionaryPair o1, DictionaryPair o2) {
                if (o1 == null && o2 == null) { return 0; }
                if (o1 == null) { return 1; }
                if (o2 == null) { return -1; }
                return o1.compareTo(o2);
            }
        });
    }

    //This function is used to moves the list of pointer down by a given amount
    private void shiftDown(Node[] pointers, int amount) {
        Node[] newPointers = new Node[this.m + 1];
        for (int i = amount; i < pointers.length; i++) {
            newPointers[i - amount] = pointers[i];
        }
        pointers = newPointers;
    }



    //This function removes all pointers within the childpointer of internalnode after specified split.
    //It returns the removed pointers in a list form .
    private Node[] splitChildPointers(InternalNode in, int split) {

        Node[] pointers = in.childPointers;
        Node[] halfPointers = new Node[this.m + 1];

        // Copy half of the values into halfPointers while updating original keys
        for (int i = split + 1; i < pointers.length; i++) {
            halfPointers[i - split - 1] = pointers[i];
            in.removePointer(i);
        }

        return halfPointers;
    }


    //This function splits the given leafnode's dictinary
    //Parameters : Leaf node's dictionary that needs to be split and an integer value ,split ,
    // which is the index at which the split happens
    private DictionaryPair[] splitDictionary(LeafNode ln, int split) {

        DictionaryPair[] dictionary = ln.dictionary;

		//Initialize a array of dictionarypairs.
        DictionaryPair[] halfDict = new DictionaryPair[this.m];

        // Copy half of the values into halfDict
        for (int i = split; i < dictionary.length; i++) {
            halfDict[i - split] = dictionary[i];
            ln.delete(i);
        }

        return halfDict;
    }



    //This function return a list of integers that are formed
    // by performing a split in the given list of key at split.
    private Integer[] splitKeys(Integer[] keys, int split) {

        Integer[] halfKeys = new Integer[this.m];

        keys[split] = null;

        //Create the output list that contains the part of list from position split till end of the given list of keys.
        for (int i = split + 1; i < keys.length; i++) {
            halfKeys[i - split - 1] = keys[i];
            keys[i] = null;
        }

        return halfKeys;
    }

    //This function is used to handle overfull nodes .
    //It handles by calling splitkeys() and splitChildPointers().
    //Parameter : An overfull internal node that needs to be split .
    private void splitInternalNode(InternalNode in) {

        // Acquire parent
        InternalNode parent = in.parent;

        // Split keys and pointers in half
        int midpoint = getMidpoint();
        int newParentKey = in.keys[midpoint];
        Integer[] halfKeys = splitKeys(in.keys, midpoint);
        Node[] halfPointers = splitChildPointers(in, midpoint);

        // Change degree of original InternalNode in
        in.degree = linearNullSearch(in.childPointers);

        // Create new sibling internal node and add half of keys and pointers
        InternalNode sibling = new InternalNode(this.m, halfKeys, halfPointers);
        for (Node pointer : halfPointers) {
            if (pointer != null) { pointer.parent = sibling; }
        }

        // Make internal nodes siblings of one another
        sibling.rightSibling = in.rightSibling;
        if (sibling.rightSibling != null) {
            sibling.rightSibling.leftSibling = sibling;
        }
        in.rightSibling = sibling;
        sibling.leftSibling = in;

        if (parent == null) {

            // Create new root node and add midpoint key and pointers
            Integer[] keys = new Integer[this.m];
            keys[0] = newParentKey;
            InternalNode newRoot = new InternalNode(this.m, keys);
            newRoot.appendChildPointer(in);
            newRoot.appendChildPointer(sibling);
            this.root = newRoot;

            // Add pointers from children to parent
            in.parent = newRoot;
            sibling.parent = newRoot;

        } else {

            // Add key to parent
            parent.keys[parent.degree - 1] = newParentKey;
            Arrays.sort(parent.keys, 0, parent.degree);

            // Set up pointer to new sibling
            int pointerIndex = parent.findIndexOfPointer(in) + 1;
            parent.insertChildPointer(sibling, pointerIndex);
            sibling.parent = parent;
        }
    }


    /* OPERATIONS : DELETE, INSERT, SEARCH */

    //This function is used to delete a pair with the given key.
    public void delete(int key) {
        if (isEmpty()) {

            //Base case: when N+ tree is empty no delete happens.

            System.err.println("Invalid Delete operation : The B+ tree is empty.");

        } else {

            //To find the leaf node that contains the given key.
            LeafNode ln = (this.root == null) ? this.first_Leaf : find_LeafNode(key);
            //Search for the pair within the leaf node with the given key.
            int dpIndex = binary_Search(ln.dictionary, ln.numPairs, key);


            if (dpIndex < 0) {

                //If index is negative then print error message

                System.err.println("Invalid Delete: Key unable to be found.");

            } else {

                //Remove the pair from the leaf node
                ln.delete(dpIndex);

                //To check for deficiency in the leaf node
                if (ln.isDeficient()) {

                    LeafNode sibling;
                    InternalNode parent = ln.parent;

                    // To check if the left sibling is lendable if yes borrow from it .
                    if (ln.leftSibling != null &&
                            ln.leftSibling.parent == ln.parent &&
                            ln.leftSibling.isLendable()) {

                        sibling = ln.leftSibling;
                        DictionaryPair borrowedDP = sibling.dictionary[sibling.numPairs - 1];

                        /* Insert borrowed dictionary pair to the leaf node, sort the dictionary,
						   and delete dictionary pair from letf sibling */
                        ln.insert(borrowedDP);
                        sortDictionary(ln.dictionary);
                        sibling.delete(sibling.numPairs - 1);

                        int pointerIndex = find_IndexOfPointer(parent.childPointers, ln);
                        if (!(borrowedDP.key >= parent.keys[pointerIndex - 1])) {
                            parent.keys[pointerIndex - 1] = ln.dictionary[0].key;
                        }

                    }
                    //IF the left sibling is not lendable check for the right sibling
                    else if (ln.rightSibling != null &&
                            ln.rightSibling.parent == ln.parent &&
                            ln.rightSibling.isLendable()) {

                        sibling = ln.rightSibling;
                        DictionaryPair borrowedDP = sibling.dictionary[0];

                        ln.insert(borrowedDP);
                        sibling.delete(0);
                        sortDictionary(sibling.dictionary);

                        int pointerIndex = find_IndexOfPointer(parent.childPointers, ln);
                        if (!(borrowedDP.key < parent.keys[pointerIndex])) {
                            parent.keys[pointerIndex] = sibling.dictionary[0].key;
                        }

                    }

                    // If both the right and left sibling are not lendable then merge them.
                    else if (ln.leftSibling != null &&
                            ln.leftSibling.parent == ln.parent &&
                            ln.leftSibling.isMergeable()) {

                        sibling = ln.leftSibling;
                        int pointerIndex = find_IndexOfPointer(parent.childPointers, ln);

                        // Remove key and child pointer from parent
                        parent.removeKey(pointerIndex - 1);
                        parent.removePointer(ln);

                        // Update sibling pointer
                        sibling.rightSibling = ln.rightSibling;

                        // Check for deficiencies in parent if yes repeat the process till deficiency goes away
                        if (parent.isDeficient()) {
                            handle_Deficiency(parent);
                        }

                    } else if (ln.rightSibling != null &&
                            ln.rightSibling.parent == ln.parent &&
                            ln.rightSibling.isMergeable()) {

                        sibling = ln.rightSibling;
                        int pointerIndex = find_IndexOfPointer(parent.childPointers, ln);

                        parent.removeKey(pointerIndex);
                        parent.removePointer(pointerIndex);

                        sibling.leftSibling = ln.leftSibling;
                        if (sibling.leftSibling == null) {
                            first_Leaf = sibling;
                        }

                        if (parent.isDeficient()) {
                            handle_Deficiency(parent);
                        }
                    }

                } else if (this.root == null && this.first_Leaf.numPairs == 0) {
                    //if the deleted pair was only pair in the dictionary , set first_left to null
                    this.first_Leaf = null;

                } else {
                    //Sort after delete.
                    sortDictionary(ln.dictionary);

                }
            }
        }
    }

    /*
    This function inserts a dictionary pair (key , value ) into the B+ tree .
    Parameters : key : which of type integer
                 value : which is of floating point
     */

    public void insert(int key, double value){
        if (isEmpty()) {

            // Starting point of execution when first insert happens

            /* a leaf node with given key and value is created and is inserted into the b+ tree.
            This is the first node in the b+ tree */
            //Root is null
            LeafNode new_leaf_node = new LeafNode(this.m, new DictionaryPair(key, value));

            // Set as first leaf node
            this.first_Leaf = new_leaf_node;

        } else {

            // Find leaf node to insert into
            LeafNode insertToLn = (this.root == null) ? this.first_Leaf :
                    find_LeafNode(key);

            // if the node is overfull , insertion fails
            if (!insertToLn.insert(new DictionaryPair(key, value))) {

                //create a new dictionary pair and insert
                insertToLn.dictionary[insertToLn.numPairs] = new DictionaryPair(key, value);
                insertToLn.numPairs++;
                sortDictionary(insertToLn.dictionary); // sort all dictionary pair along with the inserted pair

                // Divide the sorted pairs into two halves
                int middle = getMidpoint();
                DictionaryPair[] halfDict = splitDictionary(insertToLn, middle);

                //When the parent is present
                if (insertToLn.parent != null) {

                    // Add new key to parent for proper indexing
                    int newParentKey = halfDict[0].key;
                    insertToLn.parent.keys[insertToLn.parent.degree - 1] = newParentKey;
                    Arrays.sort(insertToLn.parent.keys, 0, insertToLn.parent.degree);



                } else {
                    //Starting point of execution when the b+ tree contains a node.

                    // Create internal node to serve as parent, use dictionary midpoint key
                    Integer[] parent_keys = new Integer[this.m];
                    parent_keys[0] = halfDict[0].key;
                    InternalNode parent = new InternalNode(this.m, parent_keys);
                    insertToLn.parent = parent;
                    parent.appendChildPointer(insertToLn);

                }

                // Store the other half in a new leaf node
                LeafNode newLeafNode = new LeafNode(this.m, halfDict, insertToLn.parent);

                // Updating the child pointers of parent node by 1
                int pointerIndex = insertToLn.parent.findIndexOfPointer(insertToLn) + 1;
                insertToLn.parent.insertChildPointer(newLeafNode, pointerIndex);

                // To make the leaf nodes siblings to one and other
                newLeafNode.rightSibling = insertToLn.rightSibling;
                if (newLeafNode.rightSibling != null) {
                    newLeafNode.rightSibling.leftSibling = newLeafNode;
                }
                insertToLn.rightSibling = newLeafNode;
                newLeafNode.leftSibling = insertToLn;

                //When the root node is null
                if (this.root == null) {

                    //Making  parent the new root of the b+ tree
                    this.root = insertToLn.parent;

                } else {

					/* When parent is overfull, continue up the tree,
			   		   until there are no deficiencies in the b+ tree */
                    InternalNode parentNode = insertToLn.parent;
                    while (parentNode != null) {
                        if (parentNode.isOverfull()) {
                            splitInternalNode(parentNode);
                        } else {
                            break;
                        }
                        parentNode = parentNode.parent;
                    }
                }
            }
        }
    }

    /*
    This function return the value associated to the key in the dictionary.
    If the key is not present it returns null

    Parameters : key of type integer
    Return     : Value associated to the given key.
     */
    public Double search(int Search_key) {

        // Base case: When b+ tree has no nodes , it returns Null
        if (isEmpty()) { return null; }

        // Traverse the b+ tree to find the leaf with specified key
        LeafNode ln = (this.root == null) ? this.first_Leaf : find_LeafNode(Search_key);

        // To find the index associated with key in the dictionary of leaf node using binarysearch
        DictionaryPair[] search_dps = ln.dictionary;
        int index = binary_Search(search_dps, ln.numPairs, Search_key);

        // If index is negative , key is not in the tree
        if (index < 0) {
            return null;
        } else {
            //Key is present then return the value associated to it
            return search_dps[index].value;
        }
    }

    /*
    This function traverses the DLL of leaves in B+ tree and outputs the value for all keys in the given range
    Parameters : lowerbound key (int)  , upperbound key (int)
    Return : Values for all the keys greater than or equal to the lowerbound
    and lesser than or equal to the upper bound. */

    public ArrayList<Double> search(int lowerBound, int upperBound) {

        // Intialise a array to hold values .
        ArrayList<Double> op_values = new ArrayList<Double>();

        LeafNode currNode = this.first_Leaf;
        //Iterating through the leaf nodes
        while (currNode != null) {

            // Iterate through the dictionary of each node
            DictionaryPair dps[] = currNode.dictionary;
            for (DictionaryPair dpairs : dps) {

                //Breaking point of for loop is when a null is encountered indicating no more pairs are present
                if (dpairs == null) { break; }

                //Check if the key is in range , if yes add it to op else looping till the breaking point
                if (lowerBound <= dpairs.key && dpairs.key <= upperBound) {
                    op_values.add(dpairs.value);
                }
            }

			//Updating the current node to right sibling of the preent current node
            currNode = currNode.rightSibling;

        }

        return op_values;
    }


    //Constructor of b+ tree with a parameter m which is order of the b+ tree

    public bplustree(int m) {
        this.m = m;
        this.root = null;
    }

    //Node in b+ tree which is superclass of Internal Node and leaf node
    public class Node {
        InternalNode parent;
    }

    /*This function represents Internal nodes in the B+ tree.
    Internal nodes contain only key and helps to search , delete ,insert a pair into the tree
     */
    private class InternalNode extends Node {
        int maxDegree;
        int minDegree;
        int degree;
        InternalNode leftSibling;
        InternalNode rightSibling;
        Integer[] keys;
        Node[] childPointers;

        /*
        This function appends  a pointer that points to either an internal node object
        or leaf node object to the end of the childPointers .
        Parameter : Node pointer that is to appended to child pointers list
         */
        private void appendChildPointer(Node pointer) {
            this.childPointers[degree] = pointer;
            this.degree++;
        }

        /*
        This function returns the index of pointer within the child pointer.
        If the pointer is not found then it returns -1 */

        private int findIndexOfPointer(Node pointer) {
            for (int i = 0; i < childPointers.length; i++) {
                if (childPointers[i] == pointer) { return i; }
            }
            return -1;
        }

        /*
        This  pointer
         * Given a pointer to a Node object and an integer index, this method
         * inserts the pointer at the specified index within the childPointers
         * instance variable. As a result of the insert, some pointers may be
         * shifted to the right of the index.
         * @param pointer: the Node pointer to be inserted
         * @param index: the index at which the insert is to take place
         */
        private void insertChildPointer(Node pointer, int index) {
            for (int i = degree - 1; i >= index ;i--) {
                childPointers[i + 1] = childPointers[i];
            }
            this.childPointers[index] = pointer;
            this.degree++;
        }

        //This function is used to check if the internal node is deficient or not
        //When the degree of children falls below the allowed minimum the internal node becomes deficient
        //Return boolean true if deficient otherwise returns a false.

        private boolean isDeficient() {
            return this.degree < this.minDegree;
        }

        //This function checks if the degree of the internal node is greater than the specified minimum.
        //Retruns  : True is the Internal node has excess pairs with it . Otherwise returns a false.

        private boolean isLendable() { return this.degree > this.minDegree; }

        //To check if the internal node has a degree equal to the min degree.
        // Return : True if aboves condition if true . Otherwise returns a false.

        private boolean isMergeable() { return this.degree == this.minDegree; }

        //This function checks where the degree of the internal node is more than the maxdegree.
        //Return : Boolean true if overfull otherwise returns a false


        private boolean isOverfull() {
            return this.degree == maxDegree + 1;
        }

        // This function inserts the pointer given to the start of the childpointer

        private void prependChildPointer(Node pointer) {
            for (int i = degree - 1; i >= 0 ;i--) {
                childPointers[i + 1] = childPointers[i];
            }
            this.childPointers[0] = pointer;
            this.degree++;
        }

        //This function is used remove the key associated to the particular index given as parameter.
        //This is done by setting the key of specified index to null.


        private void removeKey(int index) { this.keys[index] = null; }

        //This function sets the childpointer at the specified index to null .
        //It also decrements the internal node degree by 1.

        private void removePointer(int index) {
            this.childPointers[index] = null;
            this.degree--;
        }

        //This function removes the given pointer from childpointers
        // and also decrements the degree of the internal node

        private void removePointer(Node pointer) {
            for (int i = 0; i < childPointers.length; i++) {
                if (childPointers[i] == pointer) { this.childPointers[i] = null; }
            }
            this.degree--;
        }

        //Internal node constructor with 2 parameter such as maximum degree
        // and list of keys that it is initialised with.
        private InternalNode(int m, Integer[] keys) {
            this.maxDegree = m;
            this.minDegree = (int)Math.ceil(m/2.0);
            this.degree = 0;
            this.keys = keys;
            this.childPointers = new Node[this.maxDegree+1];
        }

        //Internal node constructor with 3 parameters such as maximum degree , list of keys and
        // list of pointer with which the internal node needs to be instantiated.
        private InternalNode(int m, Integer[] keys, Node[] pointers) {
            this.maxDegree = m;
            this.minDegree = (int)Math.ceil(m/2.0);
            this.degree = linearNullSearch(pointers);
            this.keys = keys;
            this.childPointers = pointers;
        }
    }

    //This class is used to represent the leaf nodes in a b+ tree.
    // The leaf nodes form a doubly linked list , where every leaf node has a right and left sibling.
    //Each leaf node a value for maximum and minimum pairs it can contain in it , it also doesnot have any children.

    public class LeafNode extends Node {
        int maxNumPairs;
        int minNumPairs;
        int numPairs;
        LeafNode leftSibling;
        LeafNode rightSibling;
        DictionaryPair[] dictionary;

        //This set the pair with given index to null.

        public void delete(int index) {

            this.dictionary[index] = null;
            //Updates the number of pairs in the leaf node.
            numPairs--;
        }

        // This function inserts a pair to the leaf node .
        //Returns : True if it succeeds.Flase otherwise

        public boolean insert(DictionaryPair dp) {
            if (this.isFull()) {
                //Base case : If number:of pairs of the leaf node is equal to the max degree then returns a false
                return false;
            } else {
                //insert the dictionary pair
                this.dictionary[numPairs] = dp;
                //Incrementing the number of pairs by 1
                numPairs++;
                //Sort the dctionary
                Arrays.sort(this.dictionary, 0, numPairs);

                return true;
            }
        }

        //To check if the number of pairs is less than minimum pairs.
        public boolean isDeficient() { return numPairs < minNumPairs; }

        //To check if the no:of pairs is equal to minimum pairs
        public boolean isFull() { return numPairs == maxNumPairs; }

        //To check if the number pairs in the leaf node is greater than
        // the minimum no:of pairs required to not be deficient.
        public boolean isLendable() { return numPairs > minNumPairs; }

        //To check if the no:of pairs is equal to the minimum no:of pairs
        public boolean isMergeable() {
            return numPairs == minNumPairs;
        }

        //Constructor of 2 parameters namely maximum degree and dictionary pair
        public LeafNode(int m, DictionaryPair dp) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int)(Math.ceil(m/2) - 1);
            this.dictionary = new DictionaryPair[m];
            this.numPairs = 0;
            this.insert(dp);
        }


        //Constructor with 3 parameters namely maximum degree , list of dictionary pairs
        //  to instantiate the leaf node with and parent internal node of the new leaf node.
        public LeafNode(int m, DictionaryPair[] dps, InternalNode parent) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int)(Math.ceil(m/2) - 1);
            this.dictionary = dps;
            this.numPairs = linearNullSearch(dps);
            this.parent = parent;
        }
    }

    //This class represents the dictionary key0value pairs located within each leaf node in B+ tree.

    public class DictionaryPair implements Comparable<DictionaryPair> {
        int key;
        double value;

        //Constructor of 2 parameter , namely key of type int and value of type double.
        public DictionaryPair(int key, double value) {
            this.key = key;
            this.value = value;
        }


        //This function is used to compare dictionary pairs and return 0 if both keys are same ,
        // -1 if the key is less than the given dictionary pair's key , otherwise 1

        @Override
        public int compareTo(DictionaryPair o) {
            if (key == o.key) { return 0; }
            else if (key > o.key) { return 1; }
            else { return -1; }
        }
    }

    public static void main(String[] args) {

        // TO CHECK IF THE LENGTH OF ARGS IS CORRECT or not
        if (args.length != 1) {
            System.err.println("use to run: java bplustree <input_file_name>");
            System.exit(-1);
        }

        // read from the input file
        String input_file_name = args[0];      //storing the filename
        try {

            //Fetching the input file in the system in the specified directory
            File ip_file = new File(System.getProperty("user.dir") + "/" + input_file_name);

            Scanner scan = new Scanner(ip_file);

            // Creating a output file in which the results of search query will be written.
            FileWriter opwriter = new FileWriter("output_file.txt", false);
            boolean first_Line = true;

            // Initalising a b+ tree
            bplustree bptree = null;

            // Reading the lines in the input file and performing one B+ tree operation for each line .
            while (scan.hasNextLine()) {
                String ip_line = scan.nextLine().replace(" ", "");
                String[] operations = ip_line.split("[(,)]");

                switch (operations[0]) {

                    // Initialize a b+ tree of order m
                    case "Initialize":
                        bptree = new bplustree(Integer.parseInt(operations[1]));
                        break;

                    // Insert a key-value Dictionary pair in the B+ tree
                    case "Insert":
                        bptree.insert(Integer.parseInt(operations[1]), Double.parseDouble(operations[2]));
                        break;

                    // Delete a dictionary pair of specific key from the B+ tree
                    case "Delete":
                        bptree.delete(Integer.parseInt(operations[1]));
                        break;

                    // Perform a search of particular key or search for the given range of keys on the B+ tree
                    case "Search":
                        String search_result = "";

                        // Perform search for a specific key in the b+ tree
                        if (operations.length == 2) {
                             //Search for a specific key and store the value
                            Double value = bptree.search(Integer.parseInt(operations[1]));
                            //If the search_result is null then the dictionary pair with that key is not in the b+ tree.
                            search_result = (value == null) ? "Null" :
                                    Double.toString(value);

                        }

                        // Perform search for dictionary pairs with key in the specified range .
                        else {

                            ArrayList<Double> dic_values = bptree.search(
                                    Integer.parseInt(operations[1]),
                                    Integer.parseInt(operations[2]));

                            //Ckecking if the dictionary pair with key in the specified key range is present or not.
                            if (dic_values.size() == 0) {
                                search_result = "Null";
                            } else {
                                //To record the output in the form of string
                                for (double val : dic_values) { search_result += val + ", "; }
                                //To remove the trailing ","
                                search_result = search_result.substring(0, search_result.length() - 2);

                            }
                        }

                        // Output search_result in .txt file
                        if (first_Line) {
                            opwriter.write(search_result);
                            first_Line = false;
                        } else {
                            opwriter.write("\n" + search_result);
                        }
                        opwriter.flush();

                        break;
                    default:
                        throw new IllegalArgumentException("\"" + operations[0] +
                                "\"" + " is an invalid input.");
                }
            }

            // Close output file
            opwriter.close();

        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}