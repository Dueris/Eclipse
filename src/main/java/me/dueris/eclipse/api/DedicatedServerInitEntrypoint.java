package me.dueris.eclipse.api;

public class DedicatedServerInitEntrypoint extends AbstractGameEntrypoint<ModServerInitializer> {
	@Override
	public String id() {
		return "server";
	}
}
