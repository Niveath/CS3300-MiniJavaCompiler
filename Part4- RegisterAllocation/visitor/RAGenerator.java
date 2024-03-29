//
// Generated by JTB 1.3.2
//

package visitor;
import syntaxtree.*;
import java.util.*;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order.  Your visitors may extend this class.
 */
public class RAGenerator<A> implements GJVisitor<String, A> {
    boolean debug = false;
    int pass = 0;

    int lineNumber;
    String currentLabel;

    class liveRange {
        String tempNum;
        int start;
        int end;

        boolean isSpilled;
        String register;
        int spillLocation;

        liveRange() {
            this.tempNum = null;
            this.start = -1;
            this.end = -1;

            this.isSpilled = false;
            this.register = null;
            this.spillLocation = 0;
        }
    }

    class block {
        int lineNumber;

        Vector<String> in;
        Vector<String> out;

        Vector<String> use;
        Vector<String> def;

        String jumpLabel;
        Vector<Integer> succ;

        block() {
            this.lineNumber = -1;

            this.in = new Vector<String>();
            this.out = new Vector<String>();

            this.use = new Vector<String>();
            this.def = new Vector<String>();

            this.jumpLabel = null;
            this.succ = new Vector<Integer>();
        }
    }

    class _method {
        String name;
        String numArgs;
        int stackSize;
        boolean hasCalls;
        boolean hasSpills;
        int maxCallARgs;

        HashMap<String, Integer> labelLine; // labelName -> lineNumber

        HashMap<String, liveRange> liveRanges; // tempNumber -> liveRange
        HashMap<Integer, block> blocks; // lineNumber -> block

        _method() {
            this.name = null;
            this.numArgs = null;
            this.stackSize = 0;
            this.hasCalls = false;
            this.hasSpills = false;
            this.maxCallARgs = 0;

            this.labelLine = new HashMap<String, Integer>();

            this.liveRanges = new HashMap<String, liveRange>();
            this.blocks = new HashMap<Integer, block>();
        }
    }

    HashMap<String, _method> methods = new HashMap<String, _method>(); // methodName -> _method

    void addArguments() {
        for (Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();

            if(Integer.parseInt(currentMethod.numArgs) == 0) continue;

            // System.out.println(currentMethod.name + " : " + currentMethod.numArgs);

            block newBlock = new block();
            newBlock.lineNumber = 0;

            for(int i=0; i<Integer.parseInt(currentMethod.numArgs); i++)
                newBlock.def.add(String.valueOf(i));

            newBlock.succ.add(1);

            currentMethod.blocks.put(0, newBlock);
        }
    }

    void addSuccessors() {
        for (Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();
            for (Map.Entry<Integer, block> entry2 : currentMethod.blocks.entrySet()) {
                block currentBlock = entry2.getValue();
                if(currentBlock.jumpLabel != null) {
                    currentBlock.succ.add(currentMethod.labelLine.get(currentBlock.jumpLabel));
                }
            }
        }
    }

    void printBlocks() {
        for (Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();
            System.out.println("Method: " + currentMethod.name);
            System.out.println("Num Args: " + currentMethod.numArgs);
            System.out.println("Max Call Args: " + currentMethod.maxCallARgs);
            System.out.println("Has Calls: " + currentMethod.hasCalls);

            for (Map.Entry<Integer, block> entry2 : currentMethod.blocks.entrySet()) {
                block currentBlock = entry2.getValue();

                System.out.println("Line Number: " + currentBlock.lineNumber);
                System.out.println("Use: " + currentBlock.use);
                System.out.println("Def: " + currentBlock.def);
                System.out.println("Succ: " + currentBlock.succ);
                for(int i=0; i<currentBlock.succ.size(); i++) {
                    if(!currentMethod.blocks.containsKey(currentBlock.succ.get(i))) {
                        System.out.println("Succ " + currentBlock.succ.get(i) + " not found");
                        return;
                    }
                }
                System.out.println("In: " + currentBlock.in);
                System.out.println("Out: " + currentBlock.out);
            }
        }
    }

    void generateLiveRanges() {
        for(Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();
            boolean changed = true;
            while(changed) {
                changed = false;
                for(Map.Entry<Integer, block> entry2 : currentMethod.blocks.entrySet()) {
                    block currentBlock = entry2.getValue();

                    Vector<String> newIn = new Vector<String>();
                    Vector<String> newOut = new Vector<String>();

                    for(String temp : currentBlock.out) {
                        if(!currentBlock.def.contains(temp))
                            newIn.add(temp);
                    }

                    for(String temp : currentBlock.use) {
                        if(!newIn.contains(temp))
                            newIn.add(temp);
                    }

                    for(int succ : currentBlock.succ) {
                        block succBlock = currentMethod.blocks.get(succ);
                        for(String temp : succBlock.in) {
                            if(!newOut.contains(temp))
                                newOut.add(temp);
                        }
                    }

                    newIn.sort(null);
                    newOut.sort(null);

                    if(newIn.size() != currentBlock.in.size()) {
                        changed = true;
                        currentBlock.in = newIn;
                    }
                    else{
                        for(String temp : newIn) {
                            if(!currentBlock.in.contains(temp)) {
                                changed = true;
                                currentBlock.in = newIn;
                                break;
                            }
                        }
                    }

                    if(newOut.size() != currentBlock.out.size()) {
                        changed = true;
                        currentBlock.out = newOut;
                    }
                    else{
                        for(String temp : newOut) {
                            if(!currentBlock.out.contains(temp)) {
                                changed = true;
                                currentBlock.out = newOut;
                                break;
                            }
                        }
                    }
                }
            }

            // if(debug) 
            //     System.out.println("Method: " + currentMethod.name);

            for(Map.Entry<Integer, block> entry2 : currentMethod.blocks.entrySet()) {
                block currentBlock = entry2.getValue();

                for(String temp : currentBlock.in) {
                    if(!currentMethod.liveRanges.containsKey(temp)) {
                        liveRange newLiveRange = new liveRange();
                        newLiveRange.tempNum = temp;
                        newLiveRange.end = currentBlock.lineNumber;

                        currentMethod.liveRanges.put(temp, newLiveRange);
                    }
                    else {
                        liveRange currentLiveRange = currentMethod.liveRanges.get(temp);
                        currentLiveRange.end = Math.max(currentLiveRange.end, currentBlock.lineNumber);
                    }
                }

                for(String temp : currentBlock.out) {
                    if(!currentMethod.liveRanges.containsKey(temp)) {
                        liveRange newLiveRange = new liveRange();
                        newLiveRange.tempNum = temp;
                        newLiveRange.start = currentBlock.lineNumber;

                        currentMethod.liveRanges.put(temp, newLiveRange);
                    }
                    else {
                        liveRange currentLiveRange = currentMethod.liveRanges.get(temp);
                        currentLiveRange.start = Math.min(currentLiveRange.start, currentBlock.lineNumber);
                    }
                }
            }
        }
    }

    void printLiveRanges() {
        for(Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();
            System.out.println("Method: " + currentMethod.name);

            Vector<Integer> tempNums = new Vector<Integer>();
            for(Map.Entry<String, liveRange> entry2 : currentMethod.liveRanges.entrySet()) {
                liveRange currentLiveRange = entry2.getValue();
                tempNums.add(Integer.parseInt(currentLiveRange.tempNum));
            }

            Collections.sort(tempNums);

            for(int i=0; i<tempNums.size(); i++) {
                liveRange currentLiveRange = currentMethod.liveRanges.get(String.valueOf(tempNums.get(i)));
                System.out.println("TEMP " + currentLiveRange.tempNum + " -> " + currentLiveRange.start + " : " + currentLiveRange.end);
            }
        }
    }

    Vector<String> initializeRegisters() {
        Vector<String> regs = new Vector<String>() {{
            add("s0");
            add("s1");
            add("s2");
            add("s3");
            add("s4");
            add("s5");
            add("s6");
            add("s7");
            add("t0");
            add("t1");
            add("t2");
            add("t3");
            add("t4");
            add("t5");
            add("t6");
            add("t7");
            add("t8");
            add("t9");
        }};

        return regs;
    }

    void allocateRegisters() {
        for(Map.Entry<String, _method> entry : methods.entrySet()) {
            _method currentMethod = entry.getValue();

            Vector<String> freeRegs = initializeRegisters();
            int numRegs = freeRegs.size();
            int spillLocation = Math.max(0, (Integer.parseInt(currentMethod.numArgs) - 4));
            if(!currentMethod.name.equals("MAIN")) spillLocation += 8;
            if(currentMethod.hasCalls) spillLocation += 10;

            Comparator<liveRange> liveRangeComparator = new Comparator<liveRange>() {
                @Override
                public int compare(liveRange a, liveRange b) {
                    if(a.start == b.start) {
                        return b.end - a.end;
                    }
                    return a.start - b.start;
                }
            };

            Comparator<liveRange> liveRangeComparator2 = new Comparator<liveRange>() {
                @Override
                public int compare(liveRange a, liveRange b) {
                    return a.end - b.end;
                }
            };

            PriorityQueue<liveRange> liveRanges = new PriorityQueue<liveRange>(liveRangeComparator);

            for(Map.Entry<String, liveRange> _entry : currentMethod.liveRanges.entrySet()) {
                liveRange currentLiveRange = _entry.getValue();
                if(currentLiveRange.start == 0) { // argument to method
                    if(Integer.parseInt(currentLiveRange.tempNum) < 4)
                        liveRanges.add(currentLiveRange);
                    else {
                        currentLiveRange.isSpilled = true;
                        currentLiveRange.spillLocation = Integer.parseInt(currentLiveRange.tempNum) - 4;
                    }
                }
                else
                    liveRanges.add(currentLiveRange);
            }

            PriorityQueue<liveRange> active = new PriorityQueue<liveRange>(liveRangeComparator2);

            while(!liveRanges.isEmpty()) {
                // expire old intervals
                liveRange currentLiveRange = liveRanges.poll();
                while(!active.isEmpty() && active.peek().end <= currentLiveRange.start) {
                    liveRange r = active.poll();
                    freeRegs.add(r.register);
                }

                if(active.size() == numRegs) {
                    // Spill at interval
                    liveRange spill = active.peek();
                    if(spill.end > currentLiveRange.end) {
                        currentLiveRange.isSpilled = false;
                        currentLiveRange.register = spill.register;

                        spill.isSpilled = true;
                        spill.spillLocation = spillLocation++;
                        active.poll();

                        active.add(currentLiveRange);

                        currentMethod.hasSpills = true;
                    }
                    else {
                        currentLiveRange.isSpilled = true;
                        currentLiveRange.spillLocation = spillLocation++;
                    }
                }
                else {
                    // Allocate register
                    currentLiveRange.isSpilled = false;
                    currentLiveRange.register = freeRegs.get(0);
                    freeRegs.remove(0);

                    active.add(currentLiveRange);
                }
            }

            currentMethod.stackSize = spillLocation;

            if(debug) {
                for(Map.Entry<String, liveRange> _entry : currentMethod.liveRanges.entrySet()) {
                    liveRange currentLiveRange = _entry.getValue();
                    if(currentLiveRange.isSpilled) {
                        System.out.println("TEMP " + currentLiveRange.tempNum + " SPILLED " + currentLiveRange.spillLocation);
                    }
                    else {
                        System.out.println("TEMP " + currentLiveRange.tempNum + " ALLOCATED " + currentLiveRange.register);
                    }
                }

                System.out.println("Stack Size: " + currentMethod.stackSize);
            }
        }
    }

    void saveCalleeRegs(int startLocation) {
        for(int i=0; i<8; i++) {
            System.out.println("ASTORE SPILLEDARG " + (i + startLocation) + " s" + i);
        }
    }

    void restoreCalleeRegs(int startLocation) {
        for(int i=0; i<8; i++) {
            System.out.println("ALOAD s" + i + " SPILLEDARG " + (i + startLocation));
        }
    }

    void saveCallerRegs(int startLocation) {
        for(int i=0; i<10; i++) {
            System.out.println("ASTORE SPILLEDARG " + (i + startLocation) + " t" + i);
        }
    }

    void restoreCallerRegs(int startLocation) {
        for(int i=0; i<10; i++) {
            System.out.println("ALOAD t" + i + " SPILLEDARG " + (i + startLocation));
        }
    }

    void obtainArgs(_method currentMethod) {
        for(int i=0; i<Math.min(4, Integer.parseInt(currentMethod.numArgs)); i++) {
            if(currentMethod.liveRanges.containsKey(String.valueOf(i))) {
                liveRange currentLiveRange = currentMethod.liveRanges.get(String.valueOf(i));
                if(currentLiveRange.isSpilled)
                    System.out.println("ASTORE SPILLEDARG " + currentLiveRange.spillLocation + " a" + i);
                else
                    System.out.println("MOVE " + currentLiveRange.register + " a" + i);
            }
        }
    }

    //
    // Auto class visitors--probably don't need to be overridden.
    //
    public String visit(NodeList n, A argu) {
        String _ret=null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            e.nextElement().accept(this,argu);
            _count++;
        }
        return _ret;
    }

    public String visit(NodeListOptional n, A argu) {
        if ( n.present() ) {
            String _ret=null;
            int _count=0;
            for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            e.nextElement().accept(this,argu);
            _count++;
            }
            return _ret;
        }
        else
            return null;
    }

    public String visit(NodeOptional n, A argu) {
        if ( n.present() )
            return n.node.accept(this,argu);
        else
            return null;
    }

    public String visit(NodeSequence n, A argu) {
        String _ret=null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            e.nextElement().accept(this,argu);
            _count++;
        }
        return _ret;
    }

    public String visit(NodeToken n, A argu) { return n.toString(); }

    //
    // User-generated visitor methods below
    //

    /**
    * f0 -> "MAIN"
    * f1 -> StmtList()
    * f2 -> "END"
    * f3 -> ( Procedure() )*
    * f4 -> <EOF>
    */
    public String visit(Goal n, A argu) {
        String _ret=null;
        
        // Pass 1: Collect Live Ranges of all the temps
        pass = 0;

        _method newMethod = new _method();
        newMethod.name = "MAIN";
        newMethod.numArgs = "0";

        lineNumber = 1;
        n.f1.accept(this, (A) newMethod);

        newMethod.blocks.get(lineNumber - 1).succ.clear();

        methods.put("MAIN", newMethod);

        lineNumber = 1;
        n.f3.accept(this, argu);

        addArguments();
        addSuccessors();

        generateLiveRanges();

        if(debug) {
            printBlocks();
            printLiveRanges();
        }
        
        // Allocate registers to temps using Linear Scan Algorithm
        allocateRegisters();

        // Pass 2: Convert microIR code to miniRA code
        pass = 1;

        _method mainMethod = methods.get("MAIN");

        System.out.println(mainMethod.name + " [" + mainMethod.numArgs + "] [" + mainMethod.stackSize + "] [" + mainMethod.maxCallARgs + "]");

        lineNumber = 1;
        n.f1.accept(this, (A) mainMethod);
        System.out.println("END");

        if(mainMethod.hasSpills) 
            System.out.println("// SPILLED");
        else
            System.out.println("// NOTSPILLED");

        lineNumber = 1;
        n.f3.accept(this, argu);

        n.f4.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> ( ( Label() )? Stmt() )*
    */
    public String visit(StmtList n, A argu) {
        String _ret=null;
        
        _method currentMethod = (_method) argu;

        if(pass == 0) {
            for ( Enumeration<Node> e = n.f0.elements(); e.hasMoreElements(); ) {
                NodeSequence n1 = (NodeSequence) e.nextElement();
                NodeOptional labelNode = (NodeOptional) n1.elementAt(0);
                Stmt stmtNode = (Stmt) n1.elementAt(1);

                if(labelNode.present()) { 
                    currentLabel = labelNode.accept(this, argu);
                    currentMethod.labelLine.put(currentLabel, lineNumber);  
                }
                stmtNode.accept(this, argu);
                lineNumber++;
            }
        }
        else {
            String methodName = currentMethod.name;
            for ( Enumeration<Node> e = n.f0.elements(); e.hasMoreElements(); ) {
                NodeSequence n1 = (NodeSequence) e.nextElement();
                NodeOptional labelNode = (NodeOptional) n1.elementAt(0);
                Stmt stmtNode = (Stmt) n1.elementAt(1);

                if(labelNode.present()) {
                    System.out.println(labelNode.accept(this, argu) + "_" + methodName);
                }
                stmtNode.accept(this, argu);
                lineNumber++;
            }
        }
        return _ret;
    }

    /**
    * f0 -> Label()
    * f1 -> "["
    * f2 -> IntegerLiteral()
    * f3 -> "]"
    * f4 -> StmtExp()
    */
    public String visit(Procedure n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method newMethod = new _method();
            newMethod.name = n.f0.accept(this, argu);

            newMethod.numArgs = n.f2.accept(this, argu);

            lineNumber = 1;
            n.f4.accept(this, (A) newMethod);

            methods.put(newMethod.name, newMethod);
        }
        else {
            _method currentMethod = methods.get(n.f0.accept(this, argu));
            System.out.println(currentMethod.name + " [" + currentMethod.numArgs + "] [" + currentMethod.stackSize + "] [" + currentMethod.maxCallARgs + "]");

            int startLocation = Math.max(Integer.parseInt(currentMethod.numArgs) - 4, 0);
            saveCalleeRegs(startLocation);

            obtainArgs(currentMethod);

            lineNumber = 1;
            n.f4.accept(this, (A) currentMethod);

            restoreCalleeRegs(startLocation);

            System.out.println("END");

            if(currentMethod.hasSpills) 
                System.out.println("// SPILLED");
            else
                System.out.println("// NOTSPILLED");
        }
        return _ret;
    }

    /**
    * f0 -> NoOpStmt()
    *       | ErrorStmt()
    *       | CJumpStmt()
    *       | JumpStmt()
    *       | HStoreStmt()
    *       | HLoadStmt()
    *       | MoveStmt()
    *       | PrintStmt()
    */
    public String visit(Stmt n, A argu) {
        String _ret=null;

        n.f0.accept(this, argu);

        return _ret;
    }

    /**
    * f0 -> "NOOP"
    */
    public String visit(NoOpStmt n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method currentMethod = (_method) argu;

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;
            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else
            System.out.println("NOOP");
        return _ret;
    }

    /**
    * f0 -> "ERROR"
    */
    public String visit(ErrorStmt n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method currentMethod = (_method) argu;

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;
            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else
            System.out.println("ERROR");
        return _ret;
    }

    /**
    * f0 -> "CJUMP"
    * f1 -> Temp()
    * f2 -> Label()
    */
    public String visit(CJumpStmt n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;

        if(pass == 0) {
            String tempNum = n.f1.accept(this, argu);
            String jumpLabel = n.f2.accept(this, argu);

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;
            newBlock.use.add(tempNum);
            newBlock.jumpLabel = jumpLabel;
            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else {
            String methodName = currentMethod.name;
            String tempNum = n.f1.accept(this, argu);

            liveRange currentLiveRange = currentMethod.liveRanges.get(tempNum);
            if(currentLiveRange.isSpilled) {
                System.out.println("ALOAD v0 SPILLEDARG " + currentLiveRange.spillLocation);
                System.out.println("CJUMP v0 " + n.f2.accept(this, argu));
            }
            else
                System.out.println("CJUMP " + currentLiveRange.register + " " + n.f2.accept(this, argu) + "_" + methodName);
        }

        return _ret;
    }

    /**
    * f0 -> "JUMP"
    * f1 -> Label()
    */
    public String visit(JumpStmt n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method currentMethod = (_method) argu;

            String jumpLabel = n.f1.accept(this, argu);

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;
            newBlock.jumpLabel = jumpLabel;

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else {
            String methodName = ((_method) argu).name;
            System.out.println("JUMP " + n.f1.accept(this, argu) + "_" + methodName);
        }

        return _ret;
    }

    /**
    * f0 -> "HSTORE"
    * f1 -> Temp()
    * f2 -> IntegerLiteral()
    * f3 -> Temp()
    */
    public String visit(HStoreStmt n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;

        if(pass == 0) {
            String baseTemp = n.f1.accept(this, argu);
            String valueTemp = n.f3.accept(this, argu);

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;

            newBlock.use.add(baseTemp);
            newBlock.use.add(valueTemp);

            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else {
            String baseTemp = n.f1.accept(this, argu);
            String offset = n.f2.accept(this, argu);
            String valueTemp = n.f3.accept(this, argu);

            String baseRegister, valueRegister;

            liveRange baseTempLiveRange = currentMethod.liveRanges.get(baseTemp);
            if(baseTempLiveRange.isSpilled) {
                System.out.println("ALOAD v0 SPILLEDARG " + baseTempLiveRange.spillLocation);
                baseRegister = "v0";
            }
            else
                baseRegister = baseTempLiveRange.register;

            liveRange valueTempLiveRange = currentMethod.liveRanges.get(valueTemp);
            if(valueTempLiveRange.isSpilled) {
                System.out.println("ALOAD v1 SPILLEDARG " + valueTempLiveRange.spillLocation);
                valueRegister = "v1";
            }
            else
                valueRegister = valueTempLiveRange.register;
            
            System.out.println("HSTORE " + baseRegister + " " + offset + " " + valueRegister);
        }
        return _ret;
    }

    /**
    * f0 -> "HLOAD"
    * f1 -> Temp()
    * f2 -> Temp()
    * f3 -> IntegerLiteral()
    */
    public String visit(HLoadStmt n, A argu) {
        String _ret=null;
        
        _method currentMethod = (_method) argu;

        if(pass == 0) {
            String storeTemp = n.f1.accept(this, argu);
            String baseTemp = n.f2.accept(this, argu);

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;

            newBlock.use.add(baseTemp);
            newBlock.def.add(storeTemp);

            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);
        }
        else {
            String storeTemp = n.f1.accept(this, argu);
            String baseTemp = n.f2.accept(this, argu);
            String offset = n.f3.accept(this, argu);

            String storeRegister, baseRegister;

            if(currentMethod.liveRanges.containsKey(storeTemp)) {
                liveRange storeTempLiveRange = currentMethod.liveRanges.get(storeTemp);
                if(storeTempLiveRange.isSpilled)
                    storeRegister = "v0";
                else
                    storeRegister = storeTempLiveRange.register;

                liveRange baseTempLiveRange = currentMethod.liveRanges.get(baseTemp);
                if(baseTempLiveRange.isSpilled) {
                    System.out.println("ALOAD v1 SPILLEDARG " + baseTempLiveRange.spillLocation);
                    baseRegister = "v1";
                }
                else
                    baseRegister = baseTempLiveRange.register;

                System.out.println("HLOAD " + storeRegister + " " + baseRegister + " " + offset);
                if(storeTempLiveRange.isSpilled)
                    System.out.println("ASTORE SPILLEDARG " + storeTempLiveRange.spillLocation + " v0");
                else {
                    if(storeTempLiveRange.end < lineNumber || storeTempLiveRange.start > lineNumber)
                        System.out.println("MOVE v0 " + storeRegister);
                    else
                        System.out.println("MOVE " + storeTempLiveRange.register + " " + storeRegister);
                }
            }
            else {
                liveRange baseTempLiveRange = currentMethod.liveRanges.get(baseTemp);
                if(baseTempLiveRange.isSpilled) {
                    System.out.println("ALOAD v1 SPILLEDARG " + baseTempLiveRange.spillLocation);
                    baseRegister = "v1";
                }
                else
                    baseRegister = baseTempLiveRange.register;

                System.out.println("HLOAD v0 " + baseRegister + " " + offset);
            }
        }
        return _ret;
    }

    /**
    * f0 -> "MOVE"
    * f1 -> Temp()
    * f2 -> Exp()
    */
    public String visit(MoveStmt n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;
        if(pass == 0) {
            String tempNum = n.f1.accept(this, argu);
            
            block newBlock = new block();
            newBlock.lineNumber = lineNumber;

            newBlock.def.add(tempNum);

            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);

            n.f2.accept(this, argu);
        }
        else {
            String tempNum = n.f1.accept(this, argu);
            String exp = n.f2.accept(this, argu);

            if(currentMethod.liveRanges.containsKey(tempNum)) {
                liveRange currentLiveRange = currentMethod.liveRanges.get(tempNum);
                if(currentLiveRange.isSpilled) {
                    System.out.println("MOVE v0 " + exp);
                    System.out.println("ASTORE SPILLEDARG " + currentLiveRange.spillLocation + " v0");
                }
                else {
                    if(currentLiveRange.end < lineNumber || currentLiveRange.start > lineNumber)
                        System.out.println("MOVE v0 " + exp);
                    else
                        System.out.println("MOVE " + currentLiveRange.register + " " + exp);
                }
            }
            else
                System.out.println("MOVE v0 " + exp);
        }
        return _ret;
    }

    /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
    public String visit(PrintStmt n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method currentMethod = (_method) argu;

            block newBlock = new block();
            newBlock.lineNumber = lineNumber;

            newBlock.succ.add(lineNumber + 1);

            currentMethod.blocks.put(lineNumber, newBlock);

            n.f1.accept(this, argu);
        }
        else
            System.out.println("PRINT " + n.f1.accept(this, argu));
        return _ret;
    }

    /**
    * f0 -> Call()
    *       | HAllocate()
    *       | BinOp()
    *       | SimpleExp()
    */
    public String visit(Exp n, A argu) {
        String _ret=null;
        if(pass == 0)
            n.f0.accept(this, argu);
        else
            _ret = n.f0.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> "BEGIN"
    * f1 -> StmtList()
    * f2 -> "RETURN"
    * f3 -> SimpleExp()
    * f4 -> "END"
    */
    public String visit(StmtExp n, A argu) {
        String _ret=null;
        if(pass == 0) {
            _method currentMethod = (_method) argu;
            // lineNumber++;
            n.f1.accept(this, argu);
            
            block newBlock = new block();
            newBlock.lineNumber = lineNumber;

            currentMethod.blocks.put(lineNumber, newBlock);

            n.f3.accept(this, argu);
        }
        else {
            // lineNumber++;
            n.f1.accept(this, argu);

            String returnTemp = n.f3.accept(this, argu);

            System.out.println("MOVE v0 " + returnTemp);
        }
        return _ret;
    }

    /**
    * f0 -> "CALL"
    * f1 -> SimpleExp()
    * f2 -> "("
    * f3 -> ( Temp() )*
    * f4 -> ")"
    */
    public String visit(Call n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;

        if(pass == 0) {
            n.f1.accept(this, argu);

            currentMethod.hasCalls = true;

            block callBlock = currentMethod.blocks.get(lineNumber);
            
            
            if(n.f3.present()) {
                for ( Enumeration<Node> e = n.f3.elements(); e.hasMoreElements(); ) {
                    String tempNum = e.nextElement().accept(this,argu);
                    callBlock.use.add(tempNum);
                }
                currentMethod.maxCallARgs = Math.max(n.f3.size(), currentMethod.maxCallARgs);
            }
        }
        else {
            int startLocation = Math.max(Integer.parseInt(currentMethod.numArgs) - 4, 0);
            if(!currentMethod.name.equals("MAIN")) startLocation += 8;

            saveCallerRegs(startLocation);
            
            if(n.f3.present()) {
                int count = 0;
                for ( Enumeration<Node> e = n.f3.elements(); e.hasMoreElements(); ) {
                    String tempNum = e.nextElement().accept(this,argu);
                    liveRange tempLiveRange = currentMethod.liveRanges.get(tempNum);
                    if(count < 4) {
                        if(tempLiveRange.isSpilled)
                            System.out.println("ALOAD a" + count++ + " SPILLEDARG " + tempLiveRange.spillLocation);
                        else
                            System.out.println("MOVE a" + count++ + " " + tempLiveRange.register);
                    }
                    else {
                        if(tempLiveRange.isSpilled) {
                            System.out.println("ALOAD v0 SPILLEDARG " + tempLiveRange.spillLocation);
                            System.out.println("PASSARG " + (count++ - 3) + " v0");
                        }
                        else
                            System.out.println("PASSARG " + (count++ - 3) + " " + tempLiveRange.register);
                    }
                }
            }

            System.out.println("CALL " + n.f1.accept(this, argu));

            restoreCallerRegs(startLocation);

            _ret = "v0";
        }

        return _ret;
    }

    /**
    * f0 -> "HALLOCATE"
    * f1 -> SimpleExp()
    */
    public String visit(HAllocate n, A argu) {
        String _ret=null;
        if(pass == 0)
            n.f1.accept(this, argu);
        else
            _ret = "HALLOCATE " + n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> Operator()
    * f1 -> Temp()
    * f2 -> SimpleExp()
    */
    public String visit(BinOp n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;

        if(pass == 0) {
            String tempNum = n.f1.accept(this, argu);
            currentMethod.blocks.get(lineNumber).use.add(tempNum);

            n.f2.accept(this, argu);
        }
        else {
            String operator = n.f0.accept(this, argu);
            String tempNum = n.f1.accept(this, argu);
            String simpleExp = n.f2.accept(this, argu);

            liveRange currentLiveRange = currentMethod.liveRanges.get(tempNum);
            if(currentLiveRange.isSpilled) {
                System.out.println("ALOAD v1 SPILLEDARG " + currentLiveRange.spillLocation);
                _ret = operator + " v1 " + simpleExp;
            }
            else
                _ret = operator + " " + currentLiveRange.register + " " + simpleExp;
        }
        return _ret;
    }

    /**
    * f0 -> "LE"
    *       | "NE"
    *       | "PLUS"
    *       | "MINUS"
    *       | "TIMES"
    *       | "DIV"
    */
    public String visit(Operator n, A argu) {
        String _ret=null;
        _ret = n.f0.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> Temp()
    *       | IntegerLiteral()
    *       | Label()
    */
    public String visit(SimpleExp n, A argu) {
        String _ret=null;

        _method currentMethod = (_method) argu;

        if(pass == 0) {
            String tempNum = n.f0.accept(this, argu);
            if(n.f0.which == 0)
                currentMethod.blocks.get(lineNumber).use.add(tempNum);
        }
        else {
            String tempNum = n.f0.accept(this, argu);
            if(n.f0.which == 0) {
                liveRange currentLiveRange = currentMethod.liveRanges.get(tempNum);
                if(currentLiveRange.isSpilled) {
                    System.out.println("ALOAD v0 SPILLEDARG " + currentLiveRange.spillLocation);
                    _ret = "v0";
                }
                else
                    _ret = currentLiveRange.register;
            }
            else
                _ret = tempNum;
        }
        return _ret;
    }

    /**
    * f0 -> "TEMP"
    * f1 -> IntegerLiteral()
    */
    public String visit(Temp n, A argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        _ret = n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, A argu) {
        String _ret=null;
        _ret = n.f0.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Label n, A argu) {
        String _ret=null;
        _ret = n.f0.accept(this, argu);
        return _ret;
    }

}
