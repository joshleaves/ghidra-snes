# TODO

## Allow manual header detection

In the loader, we can use `getDefaultOptions` to set a list of load options.

```java
  @Override
  public List<Option> getDefaultOptions(ByteProvider provider, LoadSpec loadSpec, DomainObject domainObject, boolean isLoadIntoProgram, boolean mirrorFsLayout) {
    List<Option> options = super.getDefaultOptions(
        provider,
        loadSpec,
        domainObject,
        isLoadIntoProgram,
        mirrorFsLayout);

    options.add(AddAction.build(
      "Header address",
      "0x0",
      null,
      "SNES",
      "rom_header_address",
      false,
      "Controls which SNES internal header location should be used."
    ));

    return options;
  }
```

Note: The API for import options is completely being deprecated and re-done after 12.0.4, so we'll wait for a stable API.


## Popup: Add ROM address

When right-clicking a spot in the listing, an "Add ROM address" should be available, which adds a label or comment (configurable) with the ROM file equivalent offset for this memory address.
