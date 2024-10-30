package me.dueris.eclipse.api.entry;

public class DedicatedServerInitEntrypoint extends AbstractGameEntrypoint<ModServerInitializer> {
	@Override
	public String id() {
		return "server";
	}
}
