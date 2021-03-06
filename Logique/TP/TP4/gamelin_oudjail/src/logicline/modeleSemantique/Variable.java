package logicline.modeleSemantique;

import java.util.HashSet;
import java.util.Set;

public class Variable extends Formule{
	protected String name;
	
	public Variable(String name){
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public Set<String> variablesLibres() {
		Set<String> result = new HashSet<>();
		result.add(name);
		return result;
	}

	@Override
	public Formule substitue(Substitution s) {
		Formule sub = s.get(name);
		return sub == null ? this : sub;
	}

	@Override
	public boolean valeur() throws VariableLibreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Formule supprImplications() {
		return this;
	}

	@Override
	protected Formule negation() {
		return new Non(this);
	}
	
	@Override
	protected Formule entrerNegations() {
		return this;
	}

	@Override
	protected boolean contientEt() {
		return false;
	}
}
