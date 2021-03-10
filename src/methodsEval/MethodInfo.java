package methodsEval;

import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.tree.analysis.Value;

public class MethodInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private transient String owner;
	private String name;
	private String desc;
	private boolean isStatic; // determines do i need to parse the constructor of the next interesting method
	private transient Map<Integer, Value> userControlledArgPos;
	private transient int parameterCount;
	private boolean isField;
	
	public MethodInfo(String owner, String name, boolean isStatic, Map<Integer, Value> userControlledArgPos, String desc, boolean isField) {
		this.name = name;
		this.owner = owner;
		this.isStatic = isStatic;
		this.userControlledArgPos = userControlledArgPos;
		this.desc = desc;
		this.isField = isField;
	}
	
	public MethodInfo(String name, String desc, boolean isStatic, int parameterCount) { //for entry point only
		this.name = name;
		this.desc = desc;
		this.isStatic = isStatic;
		this.parameterCount = parameterCount;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean getMethodType() {
		return isStatic;
	}
	
	public Map<Integer, Value> getUserControlledArgPos() {
		return userControlledArgPos;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public int getParamCount() {
		return parameterCount;
	}
	
	public boolean getIsField() {
		return isField;
	}
	
//	@Override
//	public boolean equals(Object o) {
//		MethodInfo method = (MethodInfo) o;
//		if (method.getOwner().equals(owner) && method.getName().equals(name) && method.getDesc().equals(desc) && method.isStatic == isStatic && method.isField == isField && method.getUserControlledArgPos().size() == userControlledArgPos.size()) {
//			Iterator<Map.Entry<Integer, BasicValue>> it = method.getUserControlledArgPos().entrySet().iterator();
//			while (it.hasNext()) {
//				Map.Entry<Integer, BasicValue> arg = (Map.Entry<Integer, BasicValue>) it.next();
//				int k = arg.getKey();
//				if (!userControlledArgPos.containsKey(k)) {
//					return false; // User_Derived and User_Controlled considered same as they yield same evaluation result can be overriden to increase accuracy
//				}
//			}
//			return true;
//		}
//		return false;
//	}
	
	public static int countArgs(String desc) {
		String signature = desc.replaceAll("\\)\\S+", "");
		int numOfArgs = 0;
		String prim = "[ZCBSIFJD]";
		String nonPrim = "L(\\w+/)*[\\w\\$]+;";
		Pattern pattern = Pattern.compile(nonPrim);
		Pattern primPattern = Pattern.compile(prim);
		Matcher matcher = pattern.matcher(signature);
		while (matcher.find()) {
			numOfArgs++;
		}
		String modified = signature.replaceAll(nonPrim, "");
		Matcher m = primPattern.matcher(modified);
		while (m.find()) {
			numOfArgs++;
		}
		return numOfArgs;
	}
	
	public static String convertDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuffer sb = new StringBuffer("(");
		StringBuffer local = new StringBuffer();
		String[] arr = new String[] {"^int$", "^boolean$", "^long$*", "^short$", "^float$", "^double$*", "^char$","^byte$"};
		String brackets = "(\\[)";
		Pattern arrCount = Pattern.compile(brackets);
		
		for (int i = 0; i < params.length + 1; i++) { // +1 for returntype
			String type;
			if (i == params.length) {
				sb.append(")");
				type = method.getReturnType().getName();
				if (type.equals("void")) {
					sb.append("V");
					break;
				}
			} else {
				type = params[i].getName();
			}

			Matcher counter = arrCount.matcher(type);
			if (!counter.find()) {
				boolean isPrimitive = false;
				for (int j = 0; j < 8; j++) {
					Pattern pattern = Pattern.compile(arr[j]);
					Matcher matcher = pattern.matcher(type);
					if (matcher.find()) {
						switch (j) {
						case 0:
							local.append("I");
							break;
						case 1:
							local.append("Z");
							break;
						case 2:
							local.append("J");
							break;
						case 3:
							local.append("S");
							break;
						case 4:
							local.append("F");
							break;
						case 5:
							local.append("D");
							break;
						case 6:
							local.append("C");
							break;
						case 7:
							local.append("B");
							break;
						}
						isPrimitive = true;
						break;
					}
				}
				if (!isPrimitive) {
					String classType = type.replaceAll("\\.", "/");
					local.append("L" + classType + ";");
				}
			} else {
				String classType = type.replaceAll("\\.", "/");
				local.append(classType);
			}
		
			sb.append(local);
			local.delete(0, local.length());
		}
		
		return sb.toString();
	}
	
	public static boolean checkIsInputStream(MethodInfo mf) {
		String owner = mf.getOwner();
		String name = mf.getName();
		if (owner.equals("java/io/ObjectInputStream")) {
			Class<?> io = ObjectInputStream.class;
			Method[] methods = io.getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals(name)) {
					return true;
				}
			}
		} else if (owner.equals("java/io/Externalizable")) {
			Class<?> io = Externalizable.class;
			Method[] methods = io.getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals(name)) {
					return true;
				}
				}
		} else if (owner.equals("java/io/ObjectInputValidation")) {
			if (name.equals("validateObject")) {
				return true;
			}
		} 
		return false;
	}
}
 