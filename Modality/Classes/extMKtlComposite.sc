// /* to do:
// if identical devices, do not merge;
// make it two elementGroups,
// or merge at a lower level to get
// e.g. 8 + 8 = 16 sliders etc.
// */
//
// + MKtl {
// 	*composite { |name, devDesc|
// 		^super.new.initComp(name, devDesc);
// 	}
// 	initComp { |argName, devDesc|
// 		var componentMKtls, allCompElements;
// 		name = argName;
//
// 		if (allDevDescs.isNil) { this.class.loadAllDescs };
//
// 		if (devDesc.isKindOf(String)) {
// 			devDesc = this.class.loadCompDesc(devDesc);
// 		} {
// 			if (devDesc.isKindOf(Symbol)) {
// 				devDesc = allDevDescs[devDesc];
// 			}
// 		};
// 		devDesc.postcs;
//
// 		all.put(name, this);
// 		elementsDict = ();
//
// 		componentMKtls = devDesc[\components].postcs.collect { |desc, i|
// 			var compName, shortname, compMKtl;
//
// 			shortname = MKtl.makeShortName(desc.asString);
// 			compName = [name.asString, shortname, i].join($_).asSymbol;
//
// 			if (desc.isKindOf(Symbol)) {
// 				desc = allDevDescs[desc]
// 			};
//
// 			compMKtl = MKtl(compName, desc);
// 			elementsDict.put(compName, compMKtl).postln;
// 			compMKtl;
// 		};
//
// 		allCompElements = componentMKtls.collect(_.elements).postln;
//
// 		// hierarchical joining
// 		// elements = MKtlElementGroup("", allCompElements);
//
// 		"compElements.merge here:".postln;
// 		elements = allCompElements[0];
// 		allCompElements.drop(1).do { |elem2|
// 			elements = elements.merge(elem2)
// 		};
//
// 		elements.fillDict(elementsDict);
// 	}
//
// 	*loadCompDesc { |filename|
// 		// should complain if not now
// 		^(MKtl.defaultDeviceDescriptionFolder +/+ filename ++ ".comp.scd").load.unbubble;
// 	}
// }
//
// + MKtlElementGroup {
//
// 	merge { |group, name=""|
// 		var mynames = this.elements.collect(_.name);
// 		var othernames = group.elements.collect(_.name);
// 		var sharedNames = mynames.sect(othernames).postln;
// 		if (sharedNames.size > 0) {
// 			warn("Overlapping names: %\n".format(sharedNames))
// 		};
// 		^MKtlElementGroup(name, this.elements ++ group.elements)
//
// 	}
//
// 	fillDict { |argDict|
// 		elements.do { |el|
// 			el.postln;
// 			if (el.isKindOf(this.class)) {
// 				el.fillDict(argDict)
// 			} {
// 				// it is an element
// 				argDict.put(el.name, el);
// 			}
// 		};
// 		^argDict
// 	}
// }
