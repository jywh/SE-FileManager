package com.wenhui.filebrowser.generic;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.format.Formatter;
import android.util.Log;

import com.wenhui.filebrowser.App;

public class FileUtils {

	public static final long KB = 1024;
	public static final long MB = 1024 * KB;
	public static final long GB = 1024 * MB;
	
	public static File[] getFiles( int order, File dir ) {
		switch ( order ) {
		case 0:
			return ListByName( dir );
		case 1:
			return ListByTime( dir );
		case 2:
			return ListBySize( dir );
		case 3:
			return ListByType( dir );
		default:
			return ListByName( dir );
		}
	}

	private static FileFilter filterHidden = new FileFilter() {

		@Override
		public boolean accept( File pathname ) {
			return !pathname.isHidden();
		}
	};

	public static File[] ListByTime( File dir ) {
		if ( !dir.isDirectory() ) {
			return null;
		}
		File[] files;
		if ( App.instance().showHiddenFiles() ) {
			files = dir.listFiles();
		} else {
			files = dir.listFiles( filterHidden );
		}

		Arrays.sort( files, new Comparator< File >() {

			@Override
			public int compare( File object1, File object2 ) {

				return new Long( object2.lastModified() ).compareTo( new Long( object1.lastModified() ) );
			}
		} );
		return files;

	}

	public static ArrayList< File > filterDirectory( File[] files ) {

		ArrayList< File > onlyFiles = new ArrayList< File >();
		for ( File file : files ) {
			if ( file.isFile() )
				onlyFiles.add( file );
		}
		return onlyFiles;
	}
	
	public static File[] ListByName( File dir ) {
		if ( !dir.isDirectory() ) {
			return null;
		}
		File[] files;
		if ( App.instance().showHiddenFiles() ) {
			files = dir.listFiles();
		} else {
			files = dir.listFiles( filterHidden );
		}

		Arrays.sort( files, new Comparator< File >() {

			@Override
			public int compare( File object1, File object2 ) {
				return object1.getName().toLowerCase().compareTo( object2.getName().toLowerCase() );
			}
		} );
		return files;
	}

	public static File[] ListBySize( File dir ) {
		if ( !dir.isDirectory() ) {
			return null;
		}
		File[] files;
		if ( App.instance().showHiddenFiles() ) {
			files = dir.listFiles();
		} else {
			files = dir.listFiles( filterHidden );
		}
		Arrays.sort( files, new Comparator< File >() {

			@Override
			public int compare( File object1, File object2 ) {
				return new Long( object1.length() ).compareTo( new Long( object2.length() ) );
			}
		} );
		return files;
	}

	public static File[] ListByType( File dir ) {
		if ( !dir.isDirectory() ) {
			return null;
		}

		File[] files;
		if ( App.instance().showHiddenFiles() ) {
			files = dir.listFiles();
		} else {
			files = dir.listFiles( filterHidden );
		}

		Arrays.sort( files, new Comparator< File >() {

			@Override
			public int compare( File object1, File object2 ) {
				String ext1 = getFileExtension( object1 );
				String ext2 = getFileExtension( object2 );

				if ( ext1.equals( ext2 ) ) {
					return object1.getName().toLowerCase().compareTo( object2.getName().toLowerCase() );
				}
				return ext1.compareTo( ext2 );
			}
		} );

		return files;

	}

	public static File[] ListByWithoutHidden( File dir ) {
		return dir.listFiles( filterHidden );
	}

	private static ArrayList< File > filesToRetrn = new ArrayList< File >();

	public static ArrayList< File > getExistFile( File fileToCheck ) {
		filesToRetrn.clear();
		checkFileExist( fileToCheck );
		return filesToRetrn;
	}

	/**
	 * this will check the folderToCheck exist, if it exists, continue to check the files inside the folder. if not, builde the
	 * folder and return;
	 * 
	 * @param fileToCheck
	 *            file to be copid or moved, it is the dst file
	 * @param curDir
	 *            directory to copy or move to
	 */
	private static void checkFileExist( File fileToCheck ) {
		if ( fileToCheck.exists() ) {
			filesToRetrn.add( fileToCheck );
			if ( fileToCheck.isDirectory() && fileToCheck.canRead() ) {
				try {

					File[] files = fileToCheck.listFiles();
					for ( File f : files ) {
						String filename = f.getName();
						File newFile = new File( fileToCheck, filename );
						checkFileExist( newFile );
					}
				} catch ( Exception e ) {

				}
			}
		}
	}

	private static ArrayList< File > oprFail = new ArrayList< File >();

	public static ArrayList< File > copyMultFiles( File src, File dst ) {
		oprFail.clear();
		copyFiles( src, dst );
		return oprFail;
	}

	private static void copyFiles( File src, File dst ) {
		if ( src.isDirectory() ) {
			if ( !dst.exists() ) {
				dst.mkdirs();
			}
			try {
				File[] allFiles = src.listFiles();
				for ( File f : allFiles ) {
					String filename = f.getName();
					File newSrc = new File( src, filename );
					File newDst = new File( dst, filename );
					copyFiles( newSrc, newDst );
				}
			} catch ( SecurityException se ) {
				oprFail.add( src );
			}
		} else if ( src.canRead() ) {
			try {
				copyFile( src, dst );
			} catch ( Exception e ) {
				oprFail.add( src );
			}
		}
	}

	private static void copyFile( File src, File dst ) throws IOException {
		FileChannel inChannel = new FileInputStream( src ).getChannel();
		FileChannel outChannel = new FileOutputStream( dst ).getChannel();
		try {
			inChannel.transferTo( 0, inChannel.size(), outChannel );
		} finally {
			if ( inChannel != null )
				inChannel.close();
			if ( outChannel != null )
				outChannel.close();
		}
	}

	public static boolean deleteFiles( File file ) {
		if ( file.isFile() )
			file.delete();
		else {
			try {
				File[] files = file.listFiles();
				for ( File f : files ) {
					deleteFiles( f );
				}
				file.delete();
			} catch ( Exception e ) {
				return false;
			}
		}
		return true;
	}

	public static boolean moveFiles( File src, File dst ) {
		oprFail.clear();
		copyFiles( src, dst );
		if ( oprFail.size() > 0 ) {
			deleteFiles( src );
			return false;
		} else {
			deleteFiles( src );
			return true;
		}
	}

	public static String getFileExtension( File file ) {
		if ( file.isDirectory() )
			return "";
		String fileName = file.getName();
		int dotPos = fileName.lastIndexOf( "." );
		String ext = ".";
		if ( dotPos >= 0 ) {
			ext = fileName.substring( dotPos ).toLowerCase();
		}
		return ext;
	}

	public static String getFileExtension( String filename ) {

		int dotIndex = filename.lastIndexOf( '.' );
		if ( dotIndex >= 0 ) {
			return filename.substring( dotIndex );
		} else {
			return "";
		}
	}

	private static long folderNum = 0, fileNum = 0;

	public static Long[] getFolderContents( File folder ) {
		folderNum = 0;
		fileNum = 0;
		countFiles( folder );
		return new Long[] { folderNum - 1, fileNum };
	}

	private static void countFiles( File file ) {
		if ( file.isFile() )
			fileNum++;
		else {
			folderNum++;
			if ( file.canRead() ) {
				try {
					File[] files = file.listFiles();
					for ( File f : files ) {
						countFiles( f );
					}
				} catch ( Exception e ) {

				}
			}
		}
	}

	public static int countFile( File file ) {
		int count = 0;
		if ( file.isFile() )
			count++;
		else {
			if ( file.canRead() ) {
				File[] files = file.listFiles();
				for ( File f : files )
					count += countFile( f );
			}
		}
		return count;
	}

	public static String getFilePermission( File file ) {
		String perm = "-";
		if ( file.isDirectory() )
			perm += "d";
		if ( file.canRead() )
			perm += "r";
		if ( file.canWrite() )
			perm += "w";
		return perm;
	}

	public static long calculateFileSize( File file ) {
		long length = 0;
		try {
			if ( file.isFile() && file.canRead() )
				length += file.length();
			else {
				File[] files = file.listFiles();
				for ( File f : files ) {
					length += calculateFileSize( f );
				}
			}
		} catch ( Exception se ) {

		}
		return length;
	}

	public static String toDetail( File file ) {
		long size = calculateFileSize( file );
		return Formatter.formatFileSize( App.instance(), size );
	}

	public static String formatFileSize( long size ){
		float realSize;
		if ( size > KB && size < MB ) {
			realSize = ( ( float ) size / KB );
			String str = String.format( "%.2f", realSize );
			return str + " KB";
		} else if ( size > MB && size < GB ) {
			realSize = ( ( float ) size / MB );
			String str = String.format( "%.2f", realSize );
			return str + " MB";
		} else if ( size < KB ) {
			return Long.toString( size ) + " B";
		} else {
			realSize = ( ( float ) size / GB );
			String str = String.format( "%.2f", realSize );
			return str + " GB";
		}
	}
	
	public static ArrayList< File > queryFiles = new ArrayList< File >();
	private static Matcher matcher;

	public static ArrayList< File > search( String query, File dir ) {
		Pattern pattern = createPattern( query );
		SearchFilter filter = new SearchFilter( pattern );
		queryFiles.clear();
		searchFiles( dir, filter, pattern );
		return queryFiles;
	}

	private static void searchFiles( File file, SearchFilter filter, Pattern pattern ) throws NullPointerException {
		if ( file.isDirectory() ) {
			matcher = pattern.matcher( file.getName() );
			if ( matcher.find() ) {
				queryFiles.add( file );
			}
			File[] files = file.listFiles( filter );
			for ( File f : files ) {
				searchFiles( f, filter, pattern );
			}
		} else {
			queryFiles.add( file );
		}
	}

	private static class SearchFilter implements FileFilter {

		Pattern pattern;

		public SearchFilter( Pattern pattern ) {
			this.pattern = pattern;

		}

		@Override
		public boolean accept( File pathname ) {
			if ( pathname.isDirectory() && !pathname.isHidden() )
				return true;
			matcher = pattern.matcher( pathname.getName() );
			return matcher.find();
		}
	}

	private static Pattern createPattern( String query ) {
		String regx = "^";
		int dotPos = query.lastIndexOf( "." );
		if ( dotPos >= 0 ) {
			String prefix = query.substring( 0, dotPos );
			String ext = query.substring( dotPos );
			regx = regx + prefix + "*." + ext + "$";
		} else {
			regx = regx + query + "*";
		}
		return Pattern.compile( regx, Pattern.CASE_INSENSITIVE );
	}

	public static boolean checkIfParentFile( File child, File parent ) {
		File childFile = child;
		while ( childFile != null ) {
			int result = childFile.compareTo( parent );
			if ( result == 0 )
				return true;
			childFile = childFile.getParentFile();
		}
		return false;
	}

	public static String createZipFolderName( File zipFile ) {
		String filename = zipFile.getName();
		int dotPos = filename.lastIndexOf( '.' );
		filename = filename.substring( 0, dotPos );
		Log.i( "filename", filename );
		return filename;
	}
	
	public static boolean isImageFile( String ext ) {
		return ext.equals( ".jpg" ) || ext.equals( ".png" ) || ext.equals( ".jpeg" ) || ext.equals( ".gif" );
	}
	
	public static boolean isApkFile( String ext ){
		return ext.equals( ".apk" );
	}
	
	public static boolean hasImage( File file ) {
		String ext = FileUtils.getFileExtension( file.getPath() );
		return isImageFile( ext ) || isApkFile( ext ) || isVideoFile( ext );
	}
	
	public static boolean isVideoFile( String ext){
		return ext.equals( ".mp4" );
	}

}
