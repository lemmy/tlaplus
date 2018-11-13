package org.lamport.tla.toolbox.tool.tlc.output.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.lamport.tla.toolbox.tool.tlc.ui.util.IModuleLocatable;

import tla2sany.st.Location;
import tlc2.TLCGlobals;

/**
 * Coverage information item
 * @author Simon Zambrovski
 * @version $Id$
 */
public class CoverageInformationItem implements IModuleLocatable
{
    private final static String MOD = " of module ";
    private final static String COLON = ": ";
    private final static String AT = "at ";

    private String locationString;
    private Location location;
    private String modelName;
    private long count;
	private int layer;

    /**
     * Creates an simple item storing information about a coverage at a certain location
     * @param location
     * @param count
     * @param modelName the name of the model for which this is coverage information
     * @param module
     */

    private CoverageInformationItem(Location location, long count, String modelName, int layer)
    {
        this.location = location;
        this.locationString = this.location.toString();
        this.count = count;
        this.modelName = modelName;
        this.layer = layer;
    }

    public CoverageInformationItem() {
	}

	public final String getModule()
    {
        return locationString.substring(locationString.indexOf(MOD) + MOD.length());
    }

    public final String getLocation()
    {
        return locationString.substring(0, locationString.indexOf(MOD));
    }

    public final long getCount()
    {
        return count;
    }

	/**
	 * If two CCI are co-located (overlapping, nested, ...), the layer indicates
	 * which one is considered more important.
	 */
	public int getLayer() {
		return layer;
	}

    /**
     * Parses the coverage information item from a string
     * @param outputMessage
     * @param modelName the name of the model for which this is coverage information
     * @return
     */
    public static CoverageInformationItem parse(String outputMessage, String modelName)
    {

        // "  line 84, col 32 to line 85, col 73 of module AtomicBakery: 1012492"
        outputMessage = outputMessage.trim();
        
        final int layer = outputMessage.lastIndexOf(TLCGlobals.coverageIndent) + 1;
        
        int index = outputMessage.indexOf(COLON);
        return new CoverageInformationItem(Location.parseLocation(outputMessage.substring(layer, index)), Long
                .parseLong(outputMessage.substring(index + COLON.length())), modelName, layer);
    }

    /**
     * Parses coverage timestamp from the string  
     * @param outputMessage
     * @return
     */
    public static String parseCoverageTimestamp(String outputMessage)
    {
        return outputMessage.substring(outputMessage.lastIndexOf(AT) + AT.length());
    }

    /**
     * The {@link Location} in the module.
     * @return
     */
    public Location getModuleLocation()
    {
        return location;
    }

    /**
     * The name of the model.
     * 
     * @return
     */
    public String getModelName()
    {
        return modelName;
    }

    private final List<CoverageInformationItem> childs = new ArrayList<>();
    
	List<CoverageInformationItem> getChildren() {
		return childs;
	}

	CoverageInformationItem addChild(CoverageInformationItem child) {
		if (child == this) {
			System.out.println("CoverageInformationItem.addChild()");
		}
		assert this != child;
		this.childs.add(child);
		return this;
	}

	CoverageInformationItem setLayer(int i) {
		this.layer = i;
		return this;
	}
	
	private Color color;

	CoverageInformationItem setColor(Color c) {
		this.color = c;
		return this;
	}

	private IRegion region;

	IRegion getRegion() {
		return this.region;
	}

	CoverageInformationItem setRegion(IRegion locationToRegion) {
		this.region = locationToRegion;
		return this;
	}

	private boolean isRoot() {
		return layer == -1;
	}

	private boolean active = false;
	
	public boolean isActive() {
		return active;
	}
	
	public void style(final TextPresentation textPresentation) {
		if (!isRoot()) {
			final StyleRange rs = new StyleRange();
			rs.start = region.getOffset();
			rs.length = region.getLength();
			rs.background = color;
			rs.data = this; //mergeStyleRange does not merge rs.data, thus track active instead.
			active = true;
			textPresentation.mergeStyleRange(rs);
		}
		for (CoverageInformationItem child : childs) {
			child.style(textPresentation);
		}
	}

	public void style(final TextPresentation textPresentation, final Color c) {
		if (!isRoot()) {
			final StyleRange rs = new StyleRange();
			rs.start = region.getOffset();
			rs.length = region.getLength();
			rs.background = c;
			active = false;
			textPresentation.replaceStyleRange(rs);
		}
		for (CoverageInformationItem child : childs) {
			child.style(textPresentation, c);
		}
	}
}
